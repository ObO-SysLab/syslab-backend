package net.diveon.backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.diveon.backend.domain.user.service.UserStatusService;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class WithdrawnUserCheckFilter extends OncePerRequestFilter {

    private final UserStatusService userStatusService;

    public WithdrawnUserCheckFilter(UserStatusService userStatusService) {
        this.userStatusService = userStatusService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof String userId) {
            try {
                if (!userStatusService.isActiveUser(Long.valueOf(userId))) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "탈퇴했거나 존재하지 않는 사용자입니다.");
                    return;
                }
            } catch (NumberFormatException e) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 사용자 정보입니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
