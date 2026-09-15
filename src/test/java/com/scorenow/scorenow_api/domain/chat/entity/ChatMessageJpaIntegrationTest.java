package com.scorenow.scorenow_api.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ChatMessageJpaIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockBean(name = "mongoMappingContext")
	private MongoMappingContext mongoMappingContext;

	@MockBean
	private JPAQueryFactory jpaQueryFactory;

	@Test
	void keeps_chat_message_and_nulls_user_id_when_user_is_physically_deleted() {
		User user = User.builder()
			.provider("test")
			.socialId("social-id")
			.nickname("score-user")
			.profileImageUrl("https://cdn.score-now.com/profiles/10.png")
			.build();
		entityManager.persist(user);
		Match match = Match.builder().apiMatchId("match-id").build();
		entityManager.persist(match);
		ChatMessage chatMessage = ChatMessage.create(match, user, "message");
		entityManager.persist(chatMessage);
		entityManager.flush();

		Long chatMessageId = chatMessage.getId();
		Long userId = user.getId();
		entityManager.remove(entityManager.getReference(User.class, userId));
		entityManager.flush();
		entityManager.clear();

		assertThat(jdbcTemplate.queryForObject(
			"select user_id from chat_messages where id = ?", Long.class, chatMessageId
		)).isNull();
		ChatMessage reloadedMessage = entityManager.find(ChatMessage.class, chatMessageId);
		assertThat(reloadedMessage).isNotNull();
		assertThat(reloadedMessage.getUser()).isNull();
		assertThat(reloadedMessage.getSenderNickname()).isEqualTo("score-user");
	}
}
