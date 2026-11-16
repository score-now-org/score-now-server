package com.scorenow.scorenow_api.domain.admin.member.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.admin.member.dto.AdminLoginRequestDto;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/v1/admin", "/admin"})
@Tag(name = "Admin - Auth", description = "관리자 인증 API")
public class AdminLoginController {
    private final String adminId;
    private final String passwordHash;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdminTokenStore tokens;

    public AdminLoginController(AdminTokenStore tokens, @Value("${admin.id:${ADMIN_ID:}}") String adminId,
            @Value("${admin.password-hash:${ADMIN_PASSWORD_HASH:}}") String passwordHash) {
        this.tokens = tokens;
        this.adminId = adminId;
        this.passwordHash = passwordHash;
    }

    @Operation(summary = "관리자 로그인")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AdminLoginRequestDto request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "관리자 비밀번호는 72바이트 이하여야 합니다.");
        }
        if (!StringUtils.hasText(adminId) || !StringUtils.hasText(passwordHash)
                || !passwordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_NOT_CONFIGURED);
        }
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), passwordHash);
        if (!adminId.equals(request.getId()) || !passwordMatches) throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        String token = UUID.randomUUID().toString();
        tokens.add(token);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "관리자 세션 확인")
    @GetMapping("/session")
    public ApiResponse<Void> session() { return ApiResponse.success(); }

    @Operation(summary = "관리자 페이지 테스트")
    @GetMapping("/test")
    public ResponseEntity<List<Map<String, Object>>> test() {
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "name", "게시글1"),
                Map.of("id", 2, "name", "게시글2")));
    }

    @Operation(summary = "관리자 로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        tokens.delete(AdminTokenStore.resolveToken(request.getHeader("Authorization")));
        return ApiResponse.success();
    }
}
