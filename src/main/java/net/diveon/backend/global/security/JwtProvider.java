package net.diveon.backend.global.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenValidityMs;
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L; // 7일

    public JwtProvider(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.token-validity-in-seconds}") long tokenValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenValidityMs = tokenValiditySeconds * 1000;
    }

    // access token 생성 (유효기간: application-local.yml token-validity-in-seconds)
    public String generateAccessToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .claim("token_use", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenValidityMs))
                .signWith(key)
                .compact();
    }

    // refresh token 생성 (유효기간: 7일)
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .claim("token_use", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_VALIDITY_MS))
                .signWith(key)
                .compact();
    }

    // General API callers accept access tokens only. Legacy tokens require a new login.
    public boolean validateToken(String token) {
        return valid(token, "access");
    }

    public boolean validateRefreshToken(String token) {
        return valid(token, "refresh");
    }

    private boolean valid(String token, String purpose) {
        try {
            claims(token, purpose);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 userId 추출
    public String getUserId(String token) {
        return claims(token, "access").getSubject();
    }

    public String getRefreshUserId(String token) {
        return claims(token, "refresh").getSubject();
    }

    private Claims claims(String token, String purpose) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .require("token_use", purpose).build().parseSignedClaims(token).getPayload();
        String subject = claims.getSubject();
        if (claims.getExpiration() == null || subject == null || !subject.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("Invalid token claims");
        }
        Long.parseLong(subject);
        return claims;
    }
}
