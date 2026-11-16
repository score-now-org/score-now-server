package com.scorenow.scorenow_api.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"user", "match"})
    @Query("""
        select c from ChatMessage c left join c.user u
        where (:memberId is null or u.id = :memberId)
          and (:matchId is null or c.match.id = :matchId)
          and (:sportId is null or c.match.sportId = :sportId)
          and (:socialId is null or lower(u.socialId) like :socialId escape '!')
          and (:nickname is null or lower(u.nickname) like :nickname escape '!')
        """)
    Page<ChatMessage> findForAdmin(@Param("memberId") Long memberId, @Param("matchId") Long matchId,
        @Param("sportId") Long sportId, @Param("socialId") String socialId,
        @Param("nickname") String nickname, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	List<ChatMessage> findByMatch_IdOrderByIdDesc(Long matchId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	List<ChatMessage> findByMatch_IdAndIdLessThanOrderByIdDesc(Long matchId, Long cursor, Pageable pageable);
}
