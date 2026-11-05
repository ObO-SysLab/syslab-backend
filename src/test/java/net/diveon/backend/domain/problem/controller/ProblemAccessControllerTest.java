package net.diveon.backend.domain.problem.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import net.diveon.backend.domain.grade.controller.GradeBoardController;
import net.diveon.backend.domain.grade.service.GradeBoardService;
import net.diveon.backend.domain.problem.service.ProblemOboService;
import net.diveon.backend.global.config.SecurityConfig;
import net.diveon.backend.global.exception.*;
import net.diveon.backend.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(ProblemAccessControllerTest.Config.class)
class ProblemAccessControllerTest {
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class, GradeBoardController.class, ProblemOboController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean GradeBoardService board() { return mock(GradeBoardService.class); }
        @Bean ProblemOboService obo() { return mock(ProblemOboService.class); }
        @Bean JwtProvider jwt() { return mock(JwtProvider.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired GradeBoardService board;
    @Autowired ProblemOboService obo;
    @Autowired JwtProvider jwt;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(board, obo, jwt);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    void login() { when(jwt.validateToken("login")).thenReturn(true); when(jwt.getUserId("login")).thenReturn("7"); }
    @Test void anonymousBoardAndOboNeverReachServices() throws Exception {
        mvc.perform(get("/api/problems/9/board")).andExpect(status().isForbidden());
        mvc.perform(get("/api/challenges/obo?id=9")).andExpect(status().isForbidden());
        verifyNoInteractions(board, obo);
    }
    @Test void boardUsesLoginPrincipalRatherThanForgedQueryUserAndReturns404() throws Exception {
        login();
        when(board.getBoard(7L, 9L, 1, 20, "all", "latest")).thenThrow(new ProblemNotFoundException());
        mvc.perform(get("/api/problems/9/board?userId=1").header("Authorization", "Bearer login"))
                .andExpect(status().isNotFound());
        verify(board).getBoard(7L, 9L, 1, 20, "all", "latest");
    }
    @Test void oboUsesLoginPrincipalAndReturns404() throws Exception {
        login(); when(obo.getOboJson(7L, 9L)).thenThrow(new ProblemNotFoundException());
        mvc.perform(get("/api/challenges/obo?id=9&userId=1").header("Authorization", "Bearer login"))
                .andExpect(status().isNotFound());
        verify(obo).getOboJson(7L, 9L);
    }
    @Test void explanationAndAuthorResponsesCannotBeCached() throws Exception {
        login();
        mvc.perform(get("/api/challenges/obo?id=9").header("Authorization", "Bearer login"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/challenges/obo/author?id=9&userId=1").header("Authorization", "Bearer login"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(obo).getAuthorOboJson(7L,9L);
    }
}
