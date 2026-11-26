package com.scorenow.scorenow_api.domain.admin.version;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAppVersionController.class)
@Import(SecurityConfig.class)
class AdminAppVersionSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean AdminAppVersionService versions;
    @MockBean AdminTokenStore tokens;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;

    @Test void requiresAdminAndValidatesInput() throws Exception {
        mvc.perform(get("/api/v1/admin/app-versions")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/app-versions/1")).andExpect(status().isUnauthorized());
        String token = "00000000-0000-0000-0000-000000000001";
        when(tokens.isValid(token)).thenReturn(true);
        mvc.perform(post("/api/v1/admin/app-versions").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"versionName\":\" \",\"platform\":\"IOS\",\"releaseDate\":\"2025-01-01\",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/admin/app-versions/1").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(versions).delete(1L);
    }

    @Test void trimsBeforeValidatingAndSavingVersion() throws Exception {
        String token = "00000000-0000-0000-0000-000000000001";
        when(tokens.isValid(token)).thenReturn(true);
        String version = "x".repeat(40);
        mvc.perform(post("/api/v1/admin/app-versions").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"versionName\":\" " + version + " \",\"platform\":\"IOS\",\"releaseDate\":\"2025-01-01\",\"content\":\" 내용 \"}"))
                .andExpect(status().isOk());
        verify(versions).create(argThat(request -> version.equals(request.versionName()) && "내용".equals(request.content())));
    }
}
