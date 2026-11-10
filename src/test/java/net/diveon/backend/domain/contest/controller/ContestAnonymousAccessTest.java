package net.diveon.backend.domain.contest.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.diveon.backend.domain.contest.service.ContestRankingService;
import net.diveon.backend.domain.contest.service.ContestService;
import net.diveon.backend.domain.contest.service.ContestSubmissionResultService;
import net.diveon.backend.domain.contest.service.ContestSubmissionStatusService;
import net.diveon.backend.domain.contest.service.ContestSubmitService;
import net.diveon.backend.global.config.SecurityConfig;
import net.diveon.backend.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Signed-out visitors (principal "anonymousUser") got a 500 on the public contest pages until 2026-11-10. */
@SpringJUnitWebConfig(ContestAnonymousAccessTest.Config.class)
class ContestAnonymousAccessTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class, ContestController.class})
    static class Config {
        @Bean ContestService contests() { return mock(ContestService.class); }
        @Bean ContestSubmitService submit() { return mock(ContestSubmitService.class); }
        @Bean ContestRankingService ranking() { return mock(ContestRankingService.class); }
        @Bean ContestSubmissionStatusService statusService() { return mock(ContestSubmissionStatusService.class); }
        @Bean ContestSubmissionResultService resultService() { return mock(ContestSubmissionResultService.class); }
        @Bean JwtProvider jwt() { return mock(JwtProvider.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired ContestService contests;
    @Autowired ContestSubmissionStatusService statusService;
    @Autowired JwtProvider jwt;
    MockMvc mvc;

    @BeforeEach void setup() {
        reset(contests, statusService, jwt); when(jwt.validateToken("login")).thenReturn(true); when(jwt.getUserId("login")).thenReturn("7");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void signedOutVisitorCanListAndOpenContests() throws Exception {
        mvc.perform(get("/api/contests")).andExpect(status().isOk());
        verify(contests).getContestList(isNull(), isNull(), isNull(), eq(1), eq(10), isNull());
        mvc.perform(get("/api/contests/3")).andExpect(status().isOk());
        verify(contests).getContestDetail(eq(3L), isNull());
    }

    @Test void signedInVisitorKeepsTheirOwnId() throws Exception {
        mvc.perform(get("/api/contests").header("Authorization", "Bearer login")).andExpect(status().isOk());
        verify(contests).getContestList(isNull(), isNull(), isNull(), eq(1), eq(10), eq(7L));
        mvc.perform(get("/api/contests/3").header("Authorization", "Bearer login")).andExpect(status().isOk());
        verify(contests).getContestDetail(3L, 7L);
    }

    @Test void personalContestDataNeedsSignInInsteadOfFailing() throws Exception {
        for (String path : new String[]{"/api/contests/me/owned", "/api/contests/3/submissions/9/status", "/api/contests/3/submissions/9/result",
                "/api/contests/3/participants", "/api/contests/3/participants/banned", "/api/contests/3/problems", "/api/contests/3/qnas"})
            mvc.perform(get(path)).andExpect(status().isForbidden());
        verifyNoInteractions(contests, statusService);
    }

    @Test void noticesStayPublic() throws Exception {
        // No notice controller is loaded here: reaching the handler lookup (404) means security let the request through.
        mvc.perform(get("/api/contests/3/notices")).andExpect(status().isNotFound());
    }
}
