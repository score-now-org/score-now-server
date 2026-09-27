package com.scorenow.scorenow_api.domain.push.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;

public interface PushSendHistoryRepository extends JpaRepository<PushSendHistory, Long> {

	Page<PushSendHistory> findByPlatformOrderByIdDesc(
		PushPlatform platform,
		Pageable pageable
	);

}
