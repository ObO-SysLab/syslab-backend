package net.diveon.backend.domain.contest.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import java.util.List;
import net.diveon.backend.domain.contest.dto.response.ContestSubmissionBoardResponse;
import net.diveon.backend.domain.contest.service.ContestSubmissionBoardService;
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

@SpringJUnitWebConfig(ContestSubmissionBoardControllerTest.Config.class)
class ContestSubmissionBoardControllerTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class, ContestSubmissionBoardController.class})
    static class Config {
        @Bean ContestSubmissionBoardService service() { return mock(ContestSubmissionBoardService.class); }
        @Bean JwtProvider jwt() { return mock(JwtProvider.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired ContestSubmissionBoardService service;
    @Autowired JwtProvider jwt;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(service, jwt); when(jwt.validateToken("login")).thenReturn(true); when(jwt.getUserId("login")).thenReturn("7");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void unauthenticatedCannotReadBoard() throws Exception {
        mvc.perform(get("/api/contests/1/problems/9/board")).andExpect(status().isForbidden());
        mvc.perform(get("/api/contests/1/problems/9/board").header("Authorization", "Bearer forged")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void authenticatedOwnerCannotBeOverriddenAndResponseIsNotCached() throws Exception {
        when(service.getBoard(7L, 1L, 9L, 1, 50)).thenReturn(new ContestSubmissionBoardResponse(1L, 9L, 2L, 100, 1, 0, 0, List.of()));
        mvc.perform(get("/api/contests/1/problems/9/board?userId=8").header("Authorization", "Bearer login"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.data.contestId").value(1)).andExpect(jsonPath("$.data.currentTotalScore").value(100));
        verify(service).getBoard(7L, 1L, 9L, 1, 50);
    }
    @Test void invalidPageSyntaxCannotReachService() throws Exception {
        mvc.perform(get("/api/contests/1/problems/9/board?page=wrong").header("Authorization", "Bearer login"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
}
