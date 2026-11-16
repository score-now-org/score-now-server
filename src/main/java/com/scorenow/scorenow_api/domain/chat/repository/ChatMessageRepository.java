package com.scorenow.scorenow_api.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, JpaSpecificationExecutor<ChatMessage> {

    @Override
    @EntityGraph(attributePaths = {"user", "match"})
    Page<ChatMessage> findAll(Specification<ChatMessage> specification, Pageable pageable);

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
