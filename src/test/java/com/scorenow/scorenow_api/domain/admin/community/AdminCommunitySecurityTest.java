package com.scorenow.scorenow_api.domain.admin.community;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.config.SecurityConfig;

@WebMvcTest(AdminCommunityController.class)
@Import(SecurityConfig.class)
class AdminCommunitySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean AdminCommunityService community;
    @MockBean AdminTokenStore tokens;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;

    @Test void communityManagementRequiresAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/community/posts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/community/comments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/community/images")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/community/posts/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/community/comments/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/community/images/1")).andExpect(status().isUnauthorized());
        String token = "00000000-0000-0000-0000-000000000001";
        when(tokens.isValid(token)).thenReturn(true);
        mvc.perform(delete("/api/v1/admin/community/posts/1").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(community).deletePost(1L);
        mvc.perform(delete("/api/v1/admin/community/images/1").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(community).deleteImage(1L);
    }
}
