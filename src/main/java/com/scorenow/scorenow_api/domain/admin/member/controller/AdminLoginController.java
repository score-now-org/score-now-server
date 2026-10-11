package com.scorenow.scorenow_api.domain.admin.member.controller;

import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.admin.member.dto.AdminLoginRequestDto;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping({"/api/v1/admin", "/admin"})
@Tag(name = "Admin - Auth", description = "관리자 인증 API")
public class AdminLoginController {

    private final String adminId;
    private final String adminPasswordHash;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdminTokenStore tokenStore;

    public AdminLoginController(AdminTokenStore tokenStore,
            @Value("${admin.id:${ADMIN_ID:}}") String adminId,
            @Value("${admin.password-hash:${ADMIN_PASSWORD_HASH:}}") String adminPasswordHash) {
        this.tokenStore = tokenStore;
        this.adminId = adminId;
        this.adminPasswordHash = adminPasswordHash;
    }

    @Operation(summary = "관리자 로그인", description = "관리자 ID/PW로 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AdminLoginRequestDto req) {
        if (req.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "관리자 비밀번호는 72바이트 이하로 입력해 주세요.");
        }
        if (!StringUtils.hasText(adminId) || !StringUtils.hasText(adminPasswordHash)
                || !adminPasswordHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_NOT_CONFIGURED);
        }
        if (adminId.equals(req.getId()) && passwordEncoder.matches(req.getPassword(), adminPasswordHash)) {
            String token = UUID.randomUUID().toString();
            tokenStore.add(token);
            return ResponseEntity.ok(Map.of("token", token));
        }
        throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
    }

    @Operation(summary = "관리자 로그아웃", description = "관리자 토큰을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        tokenStore.delete(AdminTokenStore.resolveToken(request.getHeader("Authorization")));
        return ApiResponse.success();
    }


    /**
     * page 테스트 api
     *
     * @param request
     * @return
     */
    @Operation(summary = "관리자 페이지 테스트", description = "Authorization 헤더 토큰 유효성 검증 테스트용 API입니다.")
    @GetMapping("/test")
    public ResponseEntity<?> getAdminData(HttpServletRequest request) {

        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "게시글1"),
                Map.of("id", 2, "name", "게시글2")
        );

        return ResponseEntity.ok(data);
    }
}
