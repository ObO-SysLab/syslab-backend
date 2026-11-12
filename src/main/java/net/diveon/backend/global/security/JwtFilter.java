package net.diveon.backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Predicate;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    // 탈퇴한 사용자의 남은 토큰을 거른다(없으면 확인하지 않음).
    private final Predicate<String> withdrawn;

    public JwtFilter(JwtProvider jwtProvider) {
        this(jwtProvider, userId -> false);
    }

    public JwtFilter(JwtProvider jwtProvider, Predicate<String> withdrawn) {
        this.jwtProvider = jwtProvider;
        this.withdrawn = withdrawn;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            try {
                String userId = jwtProvider.getUserId(token);
                if (withdrawn.test(userId)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // A token can expire between validation and reading its subject.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer {token} 헤더에서 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
