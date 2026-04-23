package com.scorenow.scorenow_api.domain.match.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.stadium.entity.TemporaryStadium;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

	private Long stadiumId;

	@Embedded
	private TemporaryStadium temporaryStadium;

	@Builder.Default
	private String matchType = "A"; // 기본 A로 세팅

	@Builder.Default
	private boolean isManual = false;

	@Builder.Default
	private boolean isActive = true;

	private String betsApiEventId;
	private String bet365Id;

	// JPA 관계 추가 (Fetch join용)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "leagueId", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private League league;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sportId", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Sport sport;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "homeId", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Team homeTeam;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "awayId", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Team awayTeam;

	public static String generateMatchId(String sportId, String eventId) {
		return "BETS" + sportId + eventId;
	}

	public static String generateManualId(String sportId) {
		String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		return String.format("MANUAL%s%s", sportId, uuid);
	}

	public void updateSportId(String sportId) {
		this.sportId = sportId;
	}

	public void updateLeagueId(String leagueId) {
		this.leagueId = leagueId;
	}

	public void updateHomeId(String homeId) {
		this.homeId = homeId;
	}

	public void updateAwayId(String awayId) {
		this.awayId = awayId;
	}

	public void updateStartAt(LocalDateTime startAt) {
		this.startAt = startAt;
	}

	public void updateStatus(MatchStatus statusCode) {
		this.statusCode = statusCode;
	}

	public void updateHomeScore(Integer homeScore) {
		this.homeScore = homeScore;
	}

	public void updateAwayScore(Integer awayScore) {
		this.awayScore = awayScore;
	}

	public void updateIsActive(boolean isActive) {
		this.isActive = isActive;
	}

    public void updateStadiumId(Long stadiumId) {
        this.stadiumId = stadiumId;
        this.temporaryStadium = null;
    }

    public void assignTemporaryStadium(String stadiumName, String city) {
        this.stadiumId = null;  // 기존에 자동으로 매핑된 경기장 정보가 있다면 해제
        this.temporaryStadium = new TemporaryStadium(stadiumName, city);
    }

    public boolean isStadiumEmpty() {
        return stadiumId == null || temporaryStadium == null;
    }
}

