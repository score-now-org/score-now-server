package com.scorenow.scorenow_api.domain.match.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inplay_match_detection_marked")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InplayMatchDetectionMarked {

	@Id
	private String matchId;

	public InplayMatchDetectionMarked(String matchId) {
		this.matchId = matchId;
	}
}
