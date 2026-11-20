package com.scorenow.scorenow_api.domain.inquiry;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.querydsl.jpa.impl.JPAQueryFactory;

@DataJpaTest @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InquiryJpaTest {
 @Autowired InquiryRepository inquiries;
 @Autowired InquiryImageRepository images;
 @Autowired EntityManager em;
 @MockBean(name = "mongoMappingContext") MongoMappingContext mongo;
 @MockBean JPAQueryFactory queryFactory;
 @MockBean UserReferenceService users;
 @Test void imageRequiresOwnershipAndValidSignature() throws Exception {
  User owner = User.builder().provider("test").socialId("owner").build(); em.persist(owner);
  User other = User.builder().provider("test").socialId("other").build(); em.persist(other);
  Inquiry inquiry = inquiries.save(new Inquiry(owner, "제목", "내용"));
  InquiryController c = new InquiryController(inquiries, images, users);
  assertThat(c.detail(owner.getId(), inquiry.getId()).getData().title()).isEqualTo("제목");
  assertThatThrownBy(() -> c.detail(other.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThat(c.list(other.getId(), 0, 20).getData().getTotalElements()).isZero();
  byte[] png = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
  c.uploadImage(owner.getId(), inquiry.getId(), new MockMultipartFile("image", "a.png", "image/png", png));
  em.flush(); em.clear();
  assertThat(c.image(owner.getId(), inquiry.getId()).getBody()).containsExactly(png);
  assertThat(c.detail(owner.getId(), inquiry.getId()).getData().hasAttachment()).isTrue();
  assertThat(c.downloadAttachment(owner.getId(), inquiry.getId()).getBody()).containsExactly(png);
  assertThat(c.downloadImage(owner.getId(), inquiry.getId()).getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
  assertThatThrownBy(() -> c.image(other.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.downloadImage(other.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.uploadImage(owner.getId(), inquiry.getId(), new MockMultipartFile("image", "bad.png", "image/png", new byte[] {1, 2}))).isInstanceOf(BusinessException.class);
 }
 @Test void documentsArePrivateDownloadOnlyAndCanReplaceLegacyImages() throws Exception {
  User owner = User.builder().provider("test").socialId("file-owner").build(); em.persist(owner);
  User other = User.builder().provider("test").socialId("file-other").build(); em.persist(other);
  Inquiry inquiry = inquiries.save(new Inquiry(owner, "문의", "내용"));
  InquiryController c = new InquiryController(inquiries, images, users);
  byte[] png = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
  c.uploadImage(owner.getId(), inquiry.getId(), new MockMultipartFile("image", "legacy.png", "image/png", png));
  byte[] pdf = "%PDF-1.7\nexample".getBytes(StandardCharsets.UTF_8);
  c.uploadAttachment(owner.getId(), inquiry.getId(), new MockMultipartFile("file", "../안내.pdf", "application/pdf", pdf));
  em.flush(); em.clear();
  var detail = c.detail(owner.getId(), inquiry.getId()).getData();
  assertThat(detail.hasImage()).isFalse();
  assertThat(detail.hasAttachment()).isTrue();
  assertThat(detail.attachmentName()).isEqualTo("안내.pdf");
  var download = c.downloadAttachment(owner.getId(), inquiry.getId());
  assertThat(download.getBody()).containsExactly(pdf);
  assertThat(download.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
  assertThat(download.getHeaders().getContentDisposition().getFilename()).isEqualTo("안내.pdf");
  assertThat(download.getHeaders().getFirst("Content-Disposition")).contains("filename*=UTF-8''");
  assertThat(c.adminAttachment(inquiry.getId()).getBody()).containsExactly(pdf);
  assertThatThrownBy(() -> c.downloadAttachment(other.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.uploadAttachment(other.getId(), inquiry.getId(), new MockMultipartFile("file", "other.pdf", "application/pdf", pdf))).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.image(owner.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.uploadAttachment(owner.getId(), inquiry.getId(), new MockMultipartFile("file", "fake.pdf", "application/pdf", new byte[]{1,2}))).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.uploadAttachment(owner.getId(), inquiry.getId(), new MockMultipartFile("file", "bad.exe", "application/octet-stream", pdf))).isInstanceOf(BusinessException.class);
  assertThat(c.downloadAttachment(owner.getId(), inquiry.getId()).getBody()).containsExactly(pdf);
  c.uploadImage(owner.getId(), inquiry.getId(), new MockMultipartFile("image", "new.png", "image/png", png));
  em.flush(); em.clear();
  assertThat(c.detail(owner.getId(), inquiry.getId()).getData().hasImage()).isTrue();
  assertThat(c.detail(owner.getId(), inquiry.getId()).getData().attachmentName()).isNull();
  assertThat(c.downloadAttachment(owner.getId(), inquiry.getId()).getBody()).containsExactly(png);
 }
 @Test void adminCanFilterAndReplyWithSafeFormattedContent() {
  User owner = User.builder().provider("test").socialId("answer-owner").nickname("축구팬").build(); em.persist(owner);
  User other = User.builder().provider("test").socialId("answer-other").nickname("야구팬").build(); em.persist(other);
  Inquiry first = inquiries.save(new Inquiry(owner, "제목", "내용"));
  inquiries.save(new Inquiry(other, "다른 문의", "내용")); em.flush();
  InquiryController c = new InquiryController(inquiries, images, users);
  assertThat(c.adminList(0, 20, "축구", owner.getId(), null, null, "WAITING").getData().getTotalElements()).isEqualTo(1);
  assertThat(c.adminList(0, 20, null, other.getId(), null, null, "ANSWERED").getData().getTotalElements()).isZero();
  assertThatThrownBy(() -> c.answer(first.getId(), new InquiryController.Answer(null, null))).isInstanceOf(BusinessException.class);
  c.answer(first.getId(), new InquiryController.Answer(null, "<p><strong>답변</strong><script>alert(1)</script><a href='javascript:alert(1)'>링크</a></p><div>두 번째 줄</div>"));
  em.flush(); em.clear();
  var answer = c.detail(owner.getId(), first.getId()).getData();
  assertThat(answer.status()).isEqualTo("ANSWERED");
  assertThat(answer.answer()).isEqualTo("답변링크\n두 번째 줄");
  assertThat(answer.answerHtml()).contains("<strong>답변</strong>").doesNotContain("script", "javascript:");
  assertThat(c.adminList(0, 20, "축구", owner.getId(), LocalDate.now(), LocalDate.now(), "ANSWERED").getData().getTotalElements()).isEqualTo(1);
  c.answer(first.getId(), new InquiryController.Answer("일반 답변", null));
  assertThat(c.detail(owner.getId(), first.getId()).getData().answer()).isEqualTo("일반 답변");
  assertThat(c.detail(owner.getId(), first.getId()).getData().answerHtml()).isNull();
  assertThatThrownBy(() -> c.detail(other.getId(), first.getId())).isInstanceOf(BusinessException.class);
 }
}
