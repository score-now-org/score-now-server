package com.scorenow.scorenow_api.domain.admin.member.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class AdminTokenAuthenticationFilter extends OncePerRequestFilter {
    private final AdminTokenStore adminTokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = AdminTokenStore.resolveToken(request.getHeader("Authorization"));
        if (adminTokenStore.isValid(token)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        }
        filterChain.doFilter(request, response);
    }
}
