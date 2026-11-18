package com.scorenow.scorenow_api.domain.inquiry;

import static org.assertj.core.api.Assertions.*;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class InquiryAttachmentTest {
    @Test void supportedDocumentsRequireTheExpectedSignature() throws Exception {
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        byte[] zip = { 'P', 'K', 3, 4 };
        byte[] ole = { (byte)0xd0, (byte)0xcf, 0x11, (byte)0xe0, (byte)0xa1, (byte)0xb1, 0x1a, (byte)0xe1 };
        for (String name : new String[]{"sample.zip", "sample.docx", "sample.xlsx", "sample.pptx", "sample.hwpx"})
            assertThat(InquiryAttachment.read(new MockMultipartFile("file", name, "application/zip", zip)).contentType()).isEqualTo("application/zip");
        for (String name : new String[]{"sample.doc", "sample.xls", "sample.ppt", "sample.hwp"})
            assertThat(InquiryAttachment.read(new MockMultipartFile("file", name, "application/octet-stream", ole)).contentType()).isEqualTo("application/x-ole");
        for (String name : new String[]{"sample.txt", "sample.csv"})
            assertThat(InquiryAttachment.read(new MockMultipartFile("file", name, "text/plain", "내용".getBytes(StandardCharsets.UTF_8))).contentType()).isEqualTo("text/plain");
        assertThat(InquiryAttachment.read(new MockMultipartFile("file", "C:\\temp\\안내.pdf", "application/pdf", pdf)).name()).isEqualTo("안내.pdf");
        assertThatThrownBy(() -> InquiryAttachment.read(new MockMultipartFile("file", "fake.pdf", "application/pdf", zip))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> InquiryAttachment.read(new MockMultipartFile("file", "unsafe.html", "text/html", pdf))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> InquiryAttachment.read(new MockMultipartFile("file", "bad.txt", "text/plain", new byte[]{(byte)0xff}))).isInstanceOf(BusinessException.class);
    }
}
