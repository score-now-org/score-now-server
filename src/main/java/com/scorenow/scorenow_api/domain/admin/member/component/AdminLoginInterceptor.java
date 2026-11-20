package com.scorenow.scorenow_api.domain.admin.member.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminLoginInterceptor implements HandlerInterceptor {
    private final AdminTokenStore tokenStore;

    public AdminLoginInterceptor(AdminTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && tokenStore.isValid(token)) {
            return true;
        }
        response.setStatus(401);
        return false;
    }
}
