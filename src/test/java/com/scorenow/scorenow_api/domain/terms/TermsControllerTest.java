package com.scorenow.scorenow_api.domain.terms;

import static org.mockito.ArgumentMatchers.*;
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

@WebMvcTest(TermsController.class)
@Import(SecurityConfig.class)
class TermsControllerTest {
    @Autowired MockMvc mvc;
    @MockBean TermsService terms;
    @MockBean AdminTokenStore tokens;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;

    @Test void guestReadAdminAuthorizationAndValidation() throws Exception {
        when(terms.current()).thenReturn(new TermsService.Snapshot(0, false, null, null));
        String token = "f0f1a891-38bd-4b31-b064-71dce52e7cc0";
        when(tokens.isValid(token)).thenReturn(true);
        mvc.perform(get("/api/v1/terms")).andExpect(status().isOk()).andExpect(jsonPath("$.data.published").value(false));
        mvc.perform(get("/api/v1/admin/terms")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/terms/publish").contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"content\":\"test\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/terms").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/terms/publish").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/terms/publish").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"test\"}"))
                .andExpect(status().isBadRequest());
        verify(terms, never()).publish(any());
    }
}
