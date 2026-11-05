package net.diveon.backend.domain.vm.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import net.diveon.backend.domain.vm.dto.response.VmStopResponse;
import net.diveon.backend.domain.vm.service.VmService;
import net.diveon.backend.global.config.SecurityConfig;
import net.diveon.backend.global.security.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(VmControllerTest.Config.class)
class VmControllerTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class, VmController.class})
    static class Config {
        @Bean VmService service() { return mock(VmService.class); }
        @Bean JwtProvider jwt() { return mock(JwtProvider.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired VmService service;
    @Autowired JwtProvider jwt;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(service, jwt); when(jwt.validateToken("login")).thenReturn(true); when(jwt.getUserId("login")).thenReturn("7");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void missingOrInvalidSessionIsRejectedForStopAndReset() throws Exception {
        for (var body : new String[]{"{\"probId\":9}", "{\"probId\":9,\"sessionId\":0}", "{\"probId\":9,\"sessionId\":-1}"}) {
            mvc.perform(delete("/api/vm/stop").header("Authorization", "Bearer login").contentType("application/json").content(body)).andExpect(status().isBadRequest());
            mvc.perform(post("/api/vm/reset").header("Authorization", "Bearer login").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void ownerComesFromLoginAndExactSessionComesFromRequest() throws Exception {
        when(service.stopVm(eq(7L), any())).thenReturn(new VmStopResponse("success", "stopped"));
        mvc.perform(delete("/api/vm/stop").header("Authorization", "Bearer login").contentType("application/json")
            .content("{\"probId\":9,\"sessionId\":10,\"userId\":8}")).andExpect(status().isOk());
        verify(service).stopVm(eq(7L), argThat(r -> r.getSessionId() == 10 && r.getProbId() == 9));
    }
    @Test void noLoginCannotStopOrReset() throws Exception {
        mvc.perform(delete("/api/vm/stop").contentType("application/json").content("{\"probId\":9,\"sessionId\":10}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/vm/reset").contentType("application/json").content("{\"probId\":9,\"sessionId\":10}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
