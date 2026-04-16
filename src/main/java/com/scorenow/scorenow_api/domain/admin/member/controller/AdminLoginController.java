package com.scorenow.scorenow_api.domain.admin.member.controller;

import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.admin.member.dto.AdminLoginRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminLoginController {

    //    @Value("${admin.id}")
    private String adminId = "admin";

    //    @Value("${admin.password}")
    private String adminPw = "new1234!";

    private final AdminTokenStore tokenStore;

    public AdminLoginController(AdminTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequestDto req) {

        if (adminId.equals(req.getId()) && adminPw.equals(req.getPassword())) {
            String token = UUID.randomUUID().toString();
            tokenStore.add(token);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).build();
    }


    /**
     * page 테스트 api
     *
     * @param request
     * @return
     */
    @GetMapping("/test")
    public ResponseEntity<?> getAdminData(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token == null || !tokenStore.isValid(token)) {
            return ResponseEntity.status(401).build();
        }

        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "게시글1"),
                Map.of("id", 2, "name", "게시글2")
        );

        return ResponseEntity.ok(data);
    }
}
