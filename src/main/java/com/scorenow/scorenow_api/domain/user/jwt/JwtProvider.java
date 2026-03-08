package com.scorenow.scorenow_api.domain.user.jwt;

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
    private final SecretKey secretKey;
    private final long ACCESS_TOKEN_EXPIRE = 1000 * 60 * 60; // 1시간
    private final long REFRESH_TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 14; // 14일

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AccessToken 생성
     * @param id
     * @return
     */
    public String createAccessToken(Long id) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE))
                .signWith(secretKey)
                .compact();
    }

    /**
     * RefreshToken 생성 + Redis저장
     * @param id
     * @return
     */
    public String createRefreshToken(Long id) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("type"));
    }
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaims(token).get("type"));
    }


    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenTtlSeconds() {
        return REFRESH_TOKEN_EXPIRE / 1000;
    }
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

