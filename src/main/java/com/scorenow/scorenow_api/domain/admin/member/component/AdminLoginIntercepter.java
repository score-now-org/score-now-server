package com.scorenow.scorenow_api.domain.admin.member.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminLoginIntercepter implements HandlerInterceptor {
    private final AdminTokenStore tokenStore;

    public AdminLoginIntercepter(AdminTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 로그인은 통과
        if (uri.equals("/admin/login")) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && tokenStore.isValid(token)) {
            return true;
        }
        response.setStatus(401);
        return false;
    }
}
