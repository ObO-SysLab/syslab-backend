package net.diveon.backend.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    static final String KEY="local-test-only-jwt-purpose-key-not-a-real-secret";
    static final String ISSUER="diveon-test";
    final JwtProvider provider=new JwtProvider(KEY,ISSUER,3600);

    @Test void accessAndRefreshHaveExclusivePurposes() {
        String access=provider.generateAccessToken("17");
        String refresh=provider.generateRefreshToken("17");
        assertTrue(provider.validateToken(access));
        assertFalse(provider.validateRefreshToken(access));
        assertTrue(provider.validateRefreshToken(refresh));
        assertFalse(provider.validateToken(refresh));
        assertEquals("17",provider.getUserId(access));
        assertEquals("17",provider.getRefreshUserId(refresh));
        assertThrows(RuntimeException.class,()->provider.getUserId(refresh));
        assertThrows(RuntimeException.class,()->provider.getRefreshUserId(access));
    }

    @ParameterizedTest
    @ValueSource(strings={"issuer","missing-issuer","purpose","legacy","expired","missing-expiry","missing-subject","zero-subject","negative-subject","text-subject","overflow-subject","wrong-key"})
    void rejectsInvalidSignedClaims(String fault) {
        var builder=Jwts.builder().issuer(ISSUER).subject("17")
                .claim("token_use","access").expiration(new Date(System.currentTimeMillis()+60000));
        switch(fault) {
            case "issuer" -> builder.issuer("other-service");
            case "missing-issuer" -> builder.issuer(null);
            case "purpose" -> builder.claim("token_use","refresh");
            case "legacy" -> builder.claim("token_use",null);
            case "expired" -> builder.expiration(new Date(System.currentTimeMillis()-60000));
            case "missing-expiry" -> builder.expiration(null);
            case "missing-subject" -> builder.subject(null);
            case "zero-subject" -> builder.subject("0");
            case "negative-subject" -> builder.subject("-1");
            case "text-subject" -> builder.subject("admin");
            case "overflow-subject" -> builder.subject("9223372036854775808");
        }
        String signingKey=fault.equals("wrong-key")?KEY+"different":KEY;
        String token=builder.signWith(Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8))).compact();
        assertFalse(provider.validateToken(token),fault);
        if (!fault.equals("purpose"))assertFalse(provider.validateRefreshToken(token),fault);
    }

    @Test void malformedAndMissingTokensAreRejected() {
        for (String token:new String[]{null,"","not-a-token","a.b.c"}) {
            assertFalse(provider.validateToken(token));assertFalse(provider.validateRefreshToken(token));
        }
    }
}
