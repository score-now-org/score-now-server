package com.scorenow.scorenow_api.domain.user.jwt;

import com.scorenow.scorenow_api.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    //로그인시 호출
    public TokenPair createToken(Long userId) {
        String accessToken  = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Redis 저장
        refreshTokenRepository.save(
                userId,
                refreshToken,
                jwtProvider.getRefreshTokenTtlSeconds()
        );
        return new TokenPair(accessToken, refreshToken);
    }

    //RefreshToken 재발급
    public TokenPair reRefreshToken(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 토큰입니다");
        }
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("RefreshToken이 아닙니다");
        }
        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenRepository.isValid(userId, refreshToken)) {
            refreshTokenRepository.delete(userId);
            throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다");
        }
        return createToken(userId);
    }

    // 로그아웃 시 호출
    public void logout(Long id) {
        refreshTokenRepository.delete(id);
    }
}
