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
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.config.SecurityConfig;
@WebMvcTest(NoticeController.class) @Import(SecurityConfig.class)
class NoticeSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean JwtProvider jwtProvider;
 @MockBean UserRepository userRepository;
 @MockBean AdminTokenStore adminTokenStore;
 @MockBean JpaMetamodelMappingContext jpa;
 @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
 @MockBean NoticeRepository notices;
 @MockBean NoticeLockRepository noticeLock;
 @Test void accessRules() throws Exception {
  when(notices.findPublished(anyCollection(), anyBoolean(), any())).thenReturn(Page.empty());
  mvc.perform(get("/api/v1/notices")).andExpect(status().isOk());
  mvc.perform(get("/admin/notices")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/notices")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/notices/status")).andExpect(status().isUnauthorized());
  when(adminTokenStore.isValid("00000000-0000-0000-0000-000000000001")).thenReturn(true);
  when(notices.findByDeletedFalse(any())).thenReturn(Page.empty());
  mvc.perform(get("/admin/notices").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/notices").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/notices/status").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/admin/notices/1/order").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")
      .contentType("application/json").content("{\"sortOrder\":0}")).andExpect(status().isBadRequest());
  mvc.perform(put("/admin/notices/1").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")
      .contentType("application/json").content("{\"title\":\"title\",\"content\":\"content\",\"ticker\":false,\"visible\":true,\"platform\":\"ALL\",\"sortOrder\":0}"))
      .andExpect(status().isBadRequest());
 }
}
