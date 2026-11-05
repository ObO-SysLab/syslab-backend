package net.diveon.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.user.controller.UserController;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.user.service.UserService;
import net.diveon.backend.domain.user.service.UserSignUpService;
import net.diveon.backend.global.config.SecurityConfig;
import net.diveon.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Production controllers/service/filter chain/JWT/BCrypt; repository and Redis boundary mocked. */
class JwtLoginBoundaryTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    ObjectMapper mapper=new ObjectMapper();
    Map<String,String> stored=new HashMap<>();
    JwtProvider jwt;

    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean JwtProvider jwtProvider(){return new JwtProvider(JwtProviderTest.KEY,JwtProviderTest.ISSUER,3600);}
        @Bean UserRepository users(){return mock(UserRepository.class);}
        @Bean RedisTemplate<String,String> redis(){return mock(RedisTemplate.class);}
        @Bean UserService usersService(UserRepository users,PasswordEncoder encoder,JwtProvider jwt,RedisTemplate<String,String> redis){return new UserService(users,encoder,jwt,redis);}
        @Bean UserSignUpService signup(){return mock(UserSignUpService.class);}
        @Bean UserController controller(UserService users,UserSignUpService signup){return new UserController(users,signup);}
        @Bean GlobalExceptionHandler exceptions(){return new GlobalExceptionHandler();}
    }

    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
        mvc=webAppContextSetup(context).apply(springSecurity()).build();jwt=context.getBean(JwtProvider.class);
        User user=new User("test-user",context.getBean(PasswordEncoder.class).encode("test-password"),"test-nickname","test@example.invalid","test",List.of());
        ReflectionTestUtils.setField(user,"id",17L);
        when(context.getBean(UserRepository.class).findByLoginId("test-user")).thenReturn(Optional.of(user));
        RedisTemplate<String,String> redis=context.getBean(RedisTemplate.class);
        ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        doAnswer(a->{stored.put(a.getArgument(0),a.getArgument(1));return null;}).when(values).set(anyString(),anyString(),anyLong(),any(TimeUnit.class));
        when(values.get(anyString())).thenAnswer(a->stored.get(a.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(a->stored.remove(a.getArgument(0))!=null);
    }
    @AfterEach void close(){if(context!=null)context.close();}
    String[] login() throws Exception {
        var response=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"loginId\":\"test-user\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var data=mapper.readTree(response.getContentAsString()).path("data");
        return new String[]{data.path("accessToken").asText(),data.path("refreshToken").asText()};
    }
    @Test void realLoginRefreshAndLogoutRespectTokenPurpose() throws Exception {
        String[] tokens=login();assertTrue(jwt.validateToken(tokens[0]));assertTrue(jwt.validateRefreshToken(tokens[1]));
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+tokens[1])).andExpect(status().isForbidden());
        assertTrue(stored.containsKey("refresh_token:17"));
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(Map.of("refreshToken",tokens[1])))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+tokens[0])).andExpect(status().isOk());
        assertFalse(stored.containsKey("refresh_token:17"));
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(Map.of("refreshToken",tokens[1])))).andExpect(status().is4xxClientError());
    }
    @Test void accessCannotBeRefreshedEvenIfStoredAtRefreshKey() throws Exception {
        String[] tokens=login();stored.put("refresh_token:17",tokens[0]);
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(Map.of("refreshToken",tokens[0])))).andExpect(status().is4xxClientError());
    }
    @Test void unsignedAndWrongIssuerRequestsNeverReachLogout() throws Exception {
        login();String other=new JwtProvider(JwtProviderTest.KEY,"different-issuer",3600).generateAccessToken("17");
        for(String token:List.of("not-a-token",other))mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());assertTrue(stored.containsKey("refresh_token:17"));
    }
    @Test void wrongPasswordDoesNotIssueOrStoreTokens() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"loginId\":\"test-user\",\"password\":\"wrong-password\"}"))
                .andExpect(status().is4xxClientError());assertTrue(stored.isEmpty());
    }
}
