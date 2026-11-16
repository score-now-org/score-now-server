package com.scorenow.scorenow_api.domain.chat.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.chat.service.ChatReportService;
import com.scorenow.scorenow_api.domain.community.service.CommunityImageService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.config.SecurityConfig;

@WebMvcTest(ChatModerationController.class)
@Import(SecurityConfig.class)
class ChatModerationControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ChatReportService reports;
    @MockBean CommunityImageService images;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean AdminTokenStore adminTokens;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;

    @Test void reportingRequiresAnAuthenticatedActiveUser() throws Exception {
        mvc.perform(post("/api/v1/chat/messages/10/reports").contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"test\"}")).andExpect(status().isUnauthorized());
        when(jwt.getAccessTokenUserId("valid")).thenReturn(5L);
        when(users.findById(5L)).thenReturn(Optional.of(User.builder().status(UserStatus.ACTIVE).build()));
        mvc.perform(post("/api/v1/chat/messages/10/reports").header("Authorization", "Bearer valid")
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\"}"))
            .andExpect(status().isOk());
        verify(reports).report(10L, 5L, "test");
    }
}
