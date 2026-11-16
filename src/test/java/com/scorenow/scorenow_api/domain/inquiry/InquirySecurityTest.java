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
@WebMvcTest(InquiryController.class) @Import({SecurityConfig.class, WebConfig.class, AdminLoginIntercepter.class})
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
  mvc.perform(get("/admin/inquiries")).andExpect(status().isUnauthorized());
  when(adminTokenStore.isValid("valid-admin")).thenReturn(true);
  when(inquiries.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());
  mvc.perform(get("/admin/inquiries").header("Authorization", "valid-admin")).andExpect(status().isOk());
 }
}
