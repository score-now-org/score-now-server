package com.scorenow.scorenow_api.domain.help;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.config.SecurityConfig;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelpController.class)
@Import(SecurityConfig.class)
class HelpSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean HelpService help;
    @MockBean FileStorage storage;
    @MockBean AdminTokenStore tokens;
    @MockBean JwtProvider jwt;
    @MockBean UserRepository users;
    @MockBean JpaMetamodelMappingContext jpa;
    @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;

    @Test void guestReadsPublishedButCannotManage() throws Exception {
        when(help.get(true)).thenReturn(new HelpService.Snapshot(-1, false, List.of()));
        mvc.perform(get("/api/v1/help")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/help")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/help").contentType(MediaType.APPLICATION_JSON).content("{\"version\":-1,\"items\":[]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/help/publish").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isUnauthorized());
        String token = "00000000-0000-0000-0000-000000000001";
        when(tokens.isValid(token)).thenReturn(true);
        when(help.get(false)).thenReturn(new HelpService.Snapshot(-1, false, List.of()));
        mvc.perform(get("/api/v1/admin/help").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/help").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":-1,\"items\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test void uploadRejectsNonImagesAndAcceptsSupportedSignatures() throws Exception {
        String token = "00000000-0000-0000-0000-000000000001";
        when(tokens.isValid(token)).thenReturn(true);
        MockMultipartFile fake = new MockMultipartFile("image", "fake.png", "image/png", "not an image".getBytes(StandardCharsets.US_ASCII));
        mvc.perform(multipart("/api/v1/admin/help/images").file(fake)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/admin/help/images").file(fake).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);

        MockMultipartFile png = new MockMultipartFile("image", "a.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        MockMultipartFile jpeg = new MockMultipartFile("image", "a.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        MockMultipartFile webp = new MockMultipartFile("image", "a.webp", "image/webp", "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII));
        when(storage.uploadFileAndReturnKey(any(), eq("help"))).thenReturn("help/image");
        for (MockMultipartFile image : List.of(png, jpeg, webp)) {
            mvc.perform(multipart("/api/v1/admin/help/images").file(image).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        verify(storage, times(3)).uploadFileAndReturnKey(any(), eq("help"));
    }
}
