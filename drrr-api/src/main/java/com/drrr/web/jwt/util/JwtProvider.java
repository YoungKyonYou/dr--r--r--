package com.drrr.web.jwt.util;


import com.drrr.core.exception.jwt.JwtExceptionCode;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {
    private static final String issuer = "DRRR-JWT-SERVICE"; // 발급자 명칭 변경 예정
    private static final Long accessTokenExpiry = 7 * 24 * 60 * 60L; // 1주
    private static final Long refreshTokenExpiry = 14 * 24 * 60 * 60L; // 2주
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String createAccessToken(final Long id, final Instant issuanceTime) {
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuanceTime)
                .expiresAt(issuanceTime.plusSeconds(accessTokenExpiry))
                .subject(String.valueOf(id))
                .claim("id", id)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String createRefreshToken(final Long id, final Instant issuanceTime) {
        // UUID 혹은 id를 사용할지 고민중
        // UUID.fromString(LocalDateTime.now().toString()).toString();
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuanceTime)
                .expiresAt(issuanceTime.plusSeconds(refreshTokenExpiry))
                .subject(String.valueOf(id))
                .claim("id", id)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private Map<String, Object> decode(final String token) {
        return jwtDecoder.decode(token).getClaims();
    }


    public Long extractToValueFrom(final String token) {
        try {
            return Long.parseLong(this.decode(token).get("id").toString());
        } catch (JwtValidationException ex) {
            throw JwtExceptionCode.JWT_UNAUTHORIZED.newInstance();
        }
    }

    public boolean validateToken(final String token) {
        try {
            this.decode(token);
            return true;
        } catch (BadJwtException e) {
            log.error("유효하지 않은 JWT 토큰");
            return false;
        }
    }

}