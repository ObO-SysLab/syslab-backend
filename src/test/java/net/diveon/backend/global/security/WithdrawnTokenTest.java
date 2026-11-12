package net.diveon.backend.global.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class WithdrawnTokenTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Object authenticate(boolean withdrawn) throws Exception {
        JwtProvider jwt = mock(JwtProvider.class);
        when(jwt.validateToken("t")).thenReturn(true);
        when(jwt.getUserId("t")).thenReturn("13");
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer t");
        FilterChain chain = mock(FilterChain.class);
        new JwtFilter(jwt, id -> withdrawn && id.equals("13")).doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getPrincipal();
    }

    @Test
    void activeUserIsAuthenticated() throws Exception {
        assertEquals("13", authenticate(false));
    }

    @Test
    void withdrawnUserTokenIsIgnored() throws Exception {
        assertNull(authenticate(true));
    }

    @Test
    void redisFailureDoesNotBlockEveryone() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("withdrawn_user:13")).thenThrow(new RedisConnectionFailureException("down"));
        assertFalse(new WithdrawnUsers(redis).isWithdrawn("13"));
        when(redis.hasKey("withdrawn_user:14")).thenReturn(true);
        assertTrue(new WithdrawnUsers(redis).isWithdrawn("14"));
    }
}
