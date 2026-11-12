package net.diveon.backend.global.config;

import net.diveon.backend.global.security.JwtFilter;
import net.diveon.backend.global.security.JwtProvider;
import net.diveon.backend.global.security.WithdrawnUsers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectProvider<WithdrawnUsers> withdrawnUsers;

    public SecurityConfig(JwtProvider jwtProvider, ObjectProvider<WithdrawnUsers> withdrawnUsers) {
        this.jwtProvider = jwtProvider;
        this.withdrawnUsers = withdrawnUsers;
    }

    private JwtFilter jwtFilter() {
        WithdrawnUsers withdrawn = withdrawnUsers.getIfAvailable();
        return withdrawn == null ? new JwtFilter(jwtProvider) : new JwtFilter(jwtProvider, withdrawn::isWithdrawn);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // The controller authenticates these two machine-only POST routes.
                        .requestMatchers(HttpMethod.POST, "/internal/terminal/connect", "/internal/terminal/check").permitAll()
                        .requestMatchers("/local/google-login-test.html", "/api/local/auth/google/login", "/api/auth/google/login", "/api/auth/login", "/api/auth/signup", "/api/auth/refresh", "/api/auth/check-loginId", "/api/auth/check-nickname", "/api/auth/email/send", "/api/auth/email/verify", "/api/auth/email/find-id", "/api/auth/email/send-reset", "/api/auth/email/password/reset", "/api/ad/**", "/error").permitAll()
                        // 홈 '실시간 탐사 로그': 공개 문제의 최근 채점 결과는 로그인 없이 본다.
                        .requestMatchers(HttpMethod.GET, "/api/activity/recent").permitAll()
                        // 상점 상품 목록·착용 정보는 로그인 없이 본다(보유 표시는 로그인했을 때만).
                        .requestMatchers(HttpMethod.GET, "/api/store/items", "/api/store/equipped").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groups/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*/members/pending").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*/problems").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*/members").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/groups/*/posts/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/groups/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contests/*/rankings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/contests/*/problems/*/board").authenticated()
                        // Personal contest data: signed-out requests get 403 instead of failing on the user id (2026-11-10).
                        .requestMatchers(HttpMethod.GET, "/api/contests/me/**", "/api/contests/*/submissions/**",
                                "/api/contests/*/participants/**", "/api/contests/*/problems", "/api/contests/*/qnas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/contests/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
