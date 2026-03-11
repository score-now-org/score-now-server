package com.scorenow.scorenow_api.domain.user.controller;

import com.scorenow.scorenow_api.domain.user.dto.*;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.service.AuthService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * 소셜로그인 API
     * 
     * @param request
     * @return
     */
    @PostMapping("/social-login")
    public ApiResponse<SocialLoginDto> socialLogin(
            @RequestBody SocialLoginRequestDto request,
            HttpServletRequest httpRequest) {
        SocialLoginDto result = authService.socialLogin(request, httpRequest);
        return ApiResponse.success(result);
    }

    /**
     * 닉네임 설정 API
     * 
     * @param request
     * @return
     */
    @PostMapping("/register-nickname")
    public ApiResponse<RegisterNicknameDto> registerNickname(
            @AuthenticationPrincipal Long id,
            @RequestBody RegisterNicknameRequestDto request) {
        RegisterNicknameDto result = authService.registerNickname(request);
        return ApiResponse.success(result);
    }

    /**
     * 로그아웃 API
     * 
     * @return
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @AuthenticationPrincipal Long id) {
        tokenService.logout(id); // redis refreshToken 삭제
        return ApiResponse.success();
    }

    /**
     * 사용자 프로필 조회 API
     * 
     * @param id
     * @return
     */
    @GetMapping("/profile")
    public ApiResponse<UserDto> getProfile(
            @AuthenticationPrincipal Long id) {
        return ApiResponse.success(authService.getProfile(id));
    }

    /**
     * 사용자 탈퇴 API
     * 
     * @param id
     * @return
     */
    @DeleteMapping("/deactivate")
    public ApiResponse<String> deactivate(
            @AuthenticationPrincipal Long id) {
        return ApiResponse.success(authService.deactivate(id));
    }

    /**
     * 리프래시토큰 갱신 API
     * 
     * @param request
     * @return
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponseDto> refresh(
            @RequestBody RefreshTokenRequestDto request) {
        TokenResponseDto tokens = authService.refresh(request.getRefreshToken());
        return ApiResponse.success(new TokenResponseDto(
                tokens.getAccessToken(), tokens.getRefreshToken()));
    }
}
