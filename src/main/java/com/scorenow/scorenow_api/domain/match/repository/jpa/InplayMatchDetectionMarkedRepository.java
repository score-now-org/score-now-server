package com.scorenow.scorenow_api.domain.match.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.match.entity.InplayMatchDetectionMarked;

public interface InplayMatchDetectionMarkedRepository
	extends JpaRepository<InplayMatchDetectionMarked, String> {
}
