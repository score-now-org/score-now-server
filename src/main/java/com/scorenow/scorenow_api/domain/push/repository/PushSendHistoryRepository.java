package com.scorenow.scorenow_api.domain.push.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;

public interface PushSendHistoryRepository extends JpaRepository<PushSendHistory, Long> {
}
