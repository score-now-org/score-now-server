package com.scorenow.scorenow_api.domain.match.entity;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {
	@Id
	private String id; // BETS + sportId + eventId (예: BETS111275660)

	private String leagueId; // BETS + sportId + leagueId
	private String sportId;
	private String homeId;   // BETS + sportId + teamId
	private String awayId;   // BETS + sportId + teamId

	@Enumerated(EnumType.STRING)
	private MatchStatus statusCode;

	private Integer homeScore;
	private Integer awayScore;
	private LocalDateTime startAt;

	@Builder.Default
	private String matchType = "A"; // 기본 A로 세팅

	@Builder.Default
	private boolean isManual = false;

	@Builder.Default
	private boolean isActive = true;

	private String betsApiEventId;
	private String bet365Id;

	public static String generateMatchId(String sportId, String eventId) {
		return "BETS" + sportId + eventId;
	}
}

