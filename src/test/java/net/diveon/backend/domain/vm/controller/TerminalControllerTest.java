package net.diveon.backend.domain.vm.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import net.diveon.backend.domain.vm.service.TerminalAccessService;
import net.diveon.backend.global.config.SecurityConfig;
import net.diveon.backend.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(TerminalControllerTest.Config.class)
class TerminalControllerTest {
    @Configuration @EnableWebMvc @Import({SecurityConfig.class, TerminalController.class})
    static class Config {
        @Bean TerminalAccessService access() { return mock(TerminalAccessService.class); }
        @Bean JwtProvider jwt() { return mock(JwtProvider.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired TerminalAccessService access;
    @Autowired JwtProvider jwt;
    MockMvc mvc;
    String cid = "a".repeat(64);
    String token = "t".repeat(43);
    @BeforeEach void setup() {
        reset(access, jwt);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void unauthenticatedAndForgedLoginCannotIssue() throws Exception {
        for (String credential : new String[]{"", "Bearer forged"}) {
            mvc.perform(post("/api/vm/terminal-ticket").header("Authorization", credential)
                .contentType("application/json").content("{\"probId\":9,\"containerId\":\"" + cid + "\"}"))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(access);
    }
    @Test void loginPrincipalOwnsRequestAndResponseIsNotCached() throws Exception {
        when(jwt.validateToken("login")).thenReturn(true); when(jwt.getUserId("login")).thenReturn("7");
        when(access.issue(7L, 9L, cid)).thenReturn(new TerminalAccessService.Ticket(token, 12345));
        mvc.perform(post("/api/vm/terminal-ticket").header("Authorization", "Bearer login")
            .contentType("application/json").content("{\"probId\":9,\"containerId\":\"" + cid + "\",\"userId\":8}"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.ticket").value(token));
        verify(access).issue(7L, 9L, cid);
    }
    @Test void internalRouteDoesNotTreatLoginJwtAsMachineCredential() throws Exception {
        when(access.consume(null, token, cid)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(post("/internal/terminal/connect").header("Authorization", "Bearer login")
            .contentType("application/json").content("{\"ticket\":\"" + token + "\",\"containerId\":\"" + cid + "\"}"))
            .andExpect(status().isForbidden());
        verify(access).consume(null, token, cid);
    }
    @Test void machineCredentialReachesServiceAndMalformedBodyIsRejected() throws Exception {
        when(access.check("machine", token)).thenReturn(new TerminalAccessService.Status(cid, 99999));
        mvc.perform(post("/internal/terminal/check").header("X-Terminal-Service-Key", "machine")
            .contentType("application/json").content("{\"lease\":\"" + token + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.containerId").value(cid));
        mvc.perform(post("/internal/terminal/check").header("X-Terminal-Service-Key", "machine")
            .contentType("application/json").content("{\"lease\":\"bad\"}"))
            .andExpect(status().isBadRequest());
        verify(access, times(1)).check("machine", token);
    }
}
