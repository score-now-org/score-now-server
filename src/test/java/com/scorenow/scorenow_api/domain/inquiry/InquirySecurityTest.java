package com.scorenow.scorenow_api.domain.inquiry;
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
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.config.SecurityConfig;
@WebMvcTest(InquiryController.class) @Import(SecurityConfig.class)
class InquirySecurityTest {
 @Autowired MockMvc mvc;
 @MockBean JwtProvider jwtProvider;
 @MockBean UserRepository userRepository;
 @MockBean AdminTokenStore adminTokenStore;
 @MockBean JpaMetamodelMappingContext jpa;
 @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
 @MockBean InquiryRepository inquiries;
 @MockBean InquiryImageRepository images;
 @MockBean UserReferenceService users;
 @Test void accessRules() throws Exception {
  mvc.perform(get("/api/v1/inquiries")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/inquiries/1/image")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/inquiries/1/image/download")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/inquiries/1/attachment")).andExpect(status().isUnauthorized());
  mvc.perform(get("/admin/inquiries")).andExpect(status().isUnauthorized());
  mvc.perform(get("/admin/inquiries/1/image")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/inquiries")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/inquiries/1/image")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/inquiries/1/attachment")).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/v1/admin/inquiries/1/answer").contentType("application/json").content("{\"content\":\"답변\"}"))
          .andExpect(status().isUnauthorized());
  when(jwtProvider.getAccessTokenUserId("user-token")).thenReturn(1L);
  when(userRepository.existsByIdAndStatus(1L, UserStatus.ACTIVE)).thenReturn(true);
  User activeUser = mock(User.class);
  when(activeUser.getStatus()).thenReturn(UserStatus.ACTIVE);
  when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
  when(inquiries.findByUser_Id(eq(1L), any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());
  mvc.perform(get("/api/v1/inquiries").header("Authorization", "Bearer user-token")).andExpect(status().isOk());
  mvc.perform(get("/admin/inquiries").header("Authorization", "Bearer user-token")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/inquiries").header("Authorization", "Bearer user-token")).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/v1/admin/inquiries/1/answer").header("Authorization", "Bearer user-token")
          .contentType("application/json").content("{\"content\":\"답변\"}")).andExpect(status().isUnauthorized());
  when(adminTokenStore.isValid("00000000-0000-0000-0000-000000000001")).thenReturn(true);
  when(inquiries.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());
  mvc.perform(get("/admin/inquiries").header("Authorization", "00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/inquiries").header("Authorization", "00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/admin/inquiries").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/inquiries").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/inquiries").header("Authorization", "Bearer 00000000-0000-0000-0000-000000000001")
          .param("to", "+999999999-12-31")).andExpect(status().isBadRequest());
  when(inquiries.findForUpdate(1L)).thenReturn(Optional.of(mock(Inquiry.class)));
  mvc.perform(put("/api/v1/admin/inquiries/1/answer").header("Authorization", "00000000-0000-0000-0000-000000000001")
          .contentType("application/json").content("{\"contentHtml\":\"<p>답변</p>\"}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/admin/inquiries/1/answer").header("Authorization", "00000000-0000-0000-0000-000000000001")
          .contentType("application/json").content("{}")).andExpect(status().isBadRequest());
 }
}
