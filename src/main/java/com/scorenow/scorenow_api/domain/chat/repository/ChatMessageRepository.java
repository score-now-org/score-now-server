package com.scorenow.scorenow_api.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByMatch_IdOrderByIdDesc(Long matchId, Pageable pageable);

	List<ChatMessage> findByMatch_IdAndIdLessThanOrderByIdDesc(Long matchId, Long cursor, Pageable pageable);
}
