package com.scorenow.scorenow_api.external.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scorenow.scorenow_api.domain.user.dto.SocialLoginRequestDto;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SocialLoginVerifier {
    private final RestTemplate restTemplate;
    private final Set<String> googleClientIds;
    private final Set<String> appleClientIds;
    private final String kakaoAppId;
    private final JwtParser googleIdTokenParser;
    private final JwtParser appleIdTokenParser;
    private final Cache<String, JwkSet> signingKeys = Caffeine.newBuilder()
            .maximumSize(2)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public SocialLoginVerifier(RestTemplateBuilder builder,
            @Value("${auth.social.google-client-ids:${GOOGLE_CLIENT_IDS:}}") String googleClientIds,
            @Value("${auth.social.apple-client-ids:${APPLE_CLIENT_IDS:}}") String appleClientIds,
            @Value("${auth.social.kakao-app-id:${KAKAO_APP_ID:}}") String kakaoAppId) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.googleClientIds = clientIds(googleClientIds);
        this.appleClientIds = clientIds(appleClientIds);
        this.kakaoAppId = kakaoAppId.trim();
        this.googleIdTokenParser = createIdTokenParser("https://www.googleapis.com/oauth2/v3/certs");
        this.appleIdTokenParser = createIdTokenParser("https://appleid.apple.com/auth/keys");
    }

    public VerifiedUser verify(SocialLoginRequestDto request) {
        if (!StringUtils.hasText(request.getProvider())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        String provider = request.getProvider().toLowerCase(Locale.ROOT);
        try {
            return switch (provider) {
                case "google" -> verifyIdToken(provider, request.getIdToken(), googleClientIds,
                        Set.of("https://accounts.google.com", "accounts.google.com"),
                        googleIdTokenParser);
                case "apple" -> verifyIdToken(provider, request.getIdToken(), appleClientIds,
                        Set.of("https://appleid.apple.com"), appleIdTokenParser);
                case "kakao" -> verifyKakao(request.getAccessToken());
                default -> throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 소셜 로그인입니다.");
            };
        } catch (HttpClientErrorException e) {
            throw new BusinessException(e.getStatusCode().value() == 429
                    ? ErrorCode.SOCIAL_AUTH_UNAVAILABLE : ErrorCode.SOCIAL_TOKEN_INVALID);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID);
        }
    }

    private VerifiedUser verifyIdToken(String provider, String token, Set<String> clientIds,
            Set<String> issuers, JwtParser parser) {
        requireToken(token);
        if (clientIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        }
        Claims claims = parser.parseSignedClaims(token).getPayload();

        if (!issuers.contains(claims.getIssuer()) || claims.getAudience() == null
                || Collections.disjoint(clientIds, claims.getAudience())
                || claims.getExpiration() == null || !StringUtils.hasText(claims.getSubject())) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID);
        }
        return new VerifiedUser(provider, claims.getSubject(), claims.get("name", String.class));
    }

    private JwtParser createIdTokenParser(String keysUrl) {
        return Jwts.parser()
                .keyLocator(header -> {
                    if (!"RS256".equals(header.get("alg")) || !(header.get("kid") instanceof String keyId)) {
                        throw new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID);
                    }
                    return signingKey(keysUrl, keyId);
                })
                .build();
    }

    private Key signingKey(String keysUrl, String keyId) {
        JwkSet keys = signingKeys.get(keysUrl, url -> {
            String json = restTemplate.getForObject(url, String.class);
            return Jwks.setParser().build().parse(json);
        });
        return keys.getKeys().stream()
                .filter(key -> keyId.equals(key.getId()) && key instanceof PublicJwk<?>)
                .map(key -> key.toKey())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID));
    }

    private VerifiedUser verifyKakao(String token) {
        requireToken(token);
        if (!StringUtils.hasText(kakaoAppId)) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        JsonNode info = restTemplate.exchange("https://kapi.kakao.com/v1/user/access_token_info",
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
        if (info == null || !kakaoAppId.equals(info.path("app_id").asText())
                || info.path("expires_in").asLong() <= 0 || info.path("id").asLong() <= 0) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID);
        }
        return new VerifiedUser("kakao", info.path("id").asText(), null);
    }

    private void requireToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_INVALID);
        }
    }

    private Set<String> clientIds(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    public record VerifiedUser(String provider, String socialId, String name) {
    }
}
