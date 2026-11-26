package com.scorenow.scorenow_api.domain.admin.member;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.admin.member.controller.AdminLoginController;
import com.scorenow.scorenow_api.domain.admin.member.controller.AdminMemberController;
import com.scorenow.scorenow_api.domain.admin.member.service.AdminMemberService;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.config.SecurityConfig;

@WebMvcTest({AdminLoginController.class, AdminMemberController.class})
@Import(SecurityConfig.class)
class AdminAuthenticationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean AdminTokenStore tokens;
    @MockBean AdminMemberService members;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("admin.id", () -> "test-admin");
        registry.add("admin.password-hash", () -> new BCryptPasswordEncoder().encode("test-password"));
    }
    @BeforeEach void configureStore() {
        sessions.clear();
        doAnswer(invocation -> { sessions.add(invocation.getArgument(0)); return null; }).when(tokens).add(anyString());
        doAnswer(invocation -> { sessions.remove(invocation.getArgument(0)); return null; }).when(tokens).delete(anyString());
        when(tokens.isValid(anyString())).thenAnswer(invocation -> sessions.contains(invocation.getArgument(0)));
    }
    @Test void loginSessionAdminAuthorizationAndLogout() throws Exception {
        mvc.perform(get("/api/v1/admin/session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/members")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/matches")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/session").header("Authorization", "Bearer ordinary-user-jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"test-admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        String response = mvc.perform(post("/api/v1/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"test-admin\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(response).get("token").asText();
        mvc.perform(get("/api/v1/admin/session").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/admin/test").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/logout").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/session").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void inputValidationRejectsBlankPassword() throws Exception {
        mvc.perform(post("/api/v1/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"test-admin\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
