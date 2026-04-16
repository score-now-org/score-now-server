package com.scorenow.scorenow_api.domain.user.controller;

import com.scorenow.scorenow_api.domain.user.dto.*;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.service.AuthService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "스코어나우 앱 로그인 인증/인가 관련 API")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * 소셜로그인 API
     * 
     * @param request
     * @return
     */
    @Operation(summary = "소셜 로그인", description = "소셜 플랫폼(Google, Kakao 등)을 통해 로그인합니다.")
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
    @Operation(summary = "닉네임 설정", description = "사용자의 닉네임을 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")  // JWT 필요한 API에 적용
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
    @Operation(summary = "로그아웃", description = "Redis에서 RefreshToken을 삭제하고 로그아웃합니다.")
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
    @Operation(summary = "프로필 조회", description = "현재 로그인된 사용자의 프로필을 조회합니다.")
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
    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 비활성화합니다.")
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
    @Operation(summary = "토큰 갱신", description = "RefreshToken으로 AccessToken과 RefreshToken을 재발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponseDto> refresh(
            @RequestBody RefreshTokenRequestDto request) {
        TokenResponseDto tokens = authService.refresh(request.getRefreshToken());
        return ApiResponse.success(new TokenResponseDto(
                tokens.getAccessToken(), tokens.getRefreshToken()));
    }
}
