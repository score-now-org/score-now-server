package com.scorenow.scorenow_api.domain.notice;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminLoginIntercepter;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.config.SecurityConfig;
import com.scorenow.scorenow_api.global.config.WebConfig;
@WebMvcTest(NoticeController.class) @Import({SecurityConfig.class, WebConfig.class, AdminLoginIntercepter.class})
class NoticeSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean JwtProvider jwtProvider;
 @MockBean UserRepository userRepository;
 @MockBean AdminTokenStore adminTokenStore;
 @MockBean JpaMetamodelMappingContext jpa;
 @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
 @MockBean NoticeRepository notices;
 @Test void accessRules() throws Exception {
  when(notices.findPublished(anyCollection(), anyBoolean(), any())).thenReturn(Page.empty());
  mvc.perform(get("/api/v1/notices")).andExpect(status().isOk());
  mvc.perform(get("/admin/notices")).andExpect(status().isUnauthorized());
  when(adminTokenStore.isValid("valid-admin")).thenReturn(true);
  when(notices.findByDeletedFalse(any())).thenReturn(Page.empty());
  mvc.perform(get("/admin/notices").header("Authorization", "valid-admin")).andExpect(status().isOk());
 }
}
