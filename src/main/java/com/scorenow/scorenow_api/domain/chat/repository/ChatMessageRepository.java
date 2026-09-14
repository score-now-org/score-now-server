package com.scorenow.scorenow_api.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	@EntityGraph(attributePaths = "user")
	List<ChatMessage> findByMatch_IdOrderByIdDesc(Long matchId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	List<ChatMessage> findByMatch_IdAndIdLessThanOrderByIdDesc(Long matchId, Long cursor, Pageable pageable);
}
