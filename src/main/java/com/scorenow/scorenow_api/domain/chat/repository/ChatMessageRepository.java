package com.scorenow.scorenow_api.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

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
          and (:reportedOnly = false or coalesce(c.reportCount, 0) > 0)
          and (:imageOnly = false or (c.imageUrl is not null and c.deletedAt is null))
        """)
    Page<ChatMessage> findForAdmin(@Param("memberId") Long memberId, @Param("matchId") Long matchId,
        @Param("sportId") Long sportId, @Param("socialId") String socialId,
        @Param("nickname") String nickname, @Param("reportedOnly") boolean reportedOnly,
        @Param("imageOnly") boolean imageOnly, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatMessage c where c.id = :id")
    Optional<ChatMessage> findByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = "user")
	@Query("select c from ChatMessage c where c.match.id = :matchId and c.deletedAt is null order by c.id desc")
	List<ChatMessage> findByMatch_IdOrderByIdDesc(@Param("matchId") Long matchId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	@Query("select c from ChatMessage c where c.match.id = :matchId and c.id < :cursor and c.deletedAt is null order by c.id desc")
	List<ChatMessage> findByMatch_IdAndIdLessThanOrderByIdDesc(@Param("matchId") Long matchId, @Param("cursor") Long cursor, Pageable pageable);
}
