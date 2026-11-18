package com.scorenow.scorenow_api.domain.inquiry;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.mock.web.MockMultipartFile;
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
  assertThatThrownBy(() -> c.image(other.getId(), inquiry.getId())).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> c.uploadImage(owner.getId(), inquiry.getId(), new MockMultipartFile("image", "bad.png", "image/png", new byte[] {1, 2}))).isInstanceOf(BusinessException.class);
 }
}
