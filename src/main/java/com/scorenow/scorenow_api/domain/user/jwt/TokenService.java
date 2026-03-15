package com.scorenow.scorenow_api.domain.user.jwt;

import com.scorenow.scorenow_api.domain.user.repository.RefreshTokenRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 로그인시 호출
    public TokenPair createToken(Long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Redis 저장
        refreshTokenRepository.save(
                userId,
                refreshToken,
                jwtProvider.getRefreshTokenTtlSeconds());
        return new TokenPair(accessToken, refreshToken);
    }

    // RefreshToken 재발급
    public TokenPair reRefreshToken(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_TYPE_MISMATCH);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenRepository.isValid(userId, refreshToken)) {
            refreshTokenRepository.delete(userId);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        return createToken(userId);
    }

    // 로그아웃 시 호출
    public String logout(Long id) {
        refreshTokenRepository.delete(id);
        return "로그아웃 되었습니다.";
    }
}
