package com.scorenow.scorenow_api.domain.user.controller;

import com.scorenow.scorenow_api.domain.user.dto.*;
import com.scorenow.scorenow_api.domain.user.service.AuthService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * 소셜로그인 API
     * @param request
     * @return
     */
    @PostMapping("/social-login")
    public ApiResponse<SocialLoginDto> socialLogin(
            @RequestBody SocialLoginRequestDto request
    ) {
        SocialLoginDto result = authService.socialLogin(request);
        return ApiResponse.success(result);
    }

    /**
     * 닉네임 설정 API
     * @param request
     * @return
     */
    @PostMapping("/register-nickname")
    public ApiResponse<RegisterNicknameDto> registerNickname(
            @RequestBody RegisterNicknameRequestDto request
    ) {
        RegisterNicknameDto result = authService.registerNickname(request);
        return ApiResponse.success(result);
    }

    /**
     * 로그아웃 API
     * @return
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    /**
     * 사용자 프로필 조회 API
     * @param authorization
     * @return
     */
    @GetMapping("/profile")
    public ApiResponse<UserDto> getProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        // Bearer 제거
        String accessToken = authorization.replace("Bearer ", "");
        UserDto user = authService.getProfile(accessToken);
        return ApiResponse.success(user);
    }

    /**
     * 사용자 탈퇴 API
     * @param authentication
     * @return
     */
    @DeleteMapping("/deactivate")
    public ApiResponse<Void> deactivate(Authentication authentication) {

        String socialId = authentication.getName();
        authService.deactivate(socialId);

        return ApiResponse.success();
    }
}
