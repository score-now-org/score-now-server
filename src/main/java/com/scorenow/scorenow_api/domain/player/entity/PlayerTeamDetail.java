package com.scorenow.scorenow_api.domain.player.entity;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_team_details")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlayerTeamDetail extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "player_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Player player;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "team_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Team team;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "league_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private League league;

	private String season;
	private String position;

	@Column(name = "shirt_number")
	private String shirtNumber;

	@Builder.Default
	private boolean squadOn = false; // 스쿼드 on/off

	@Enumerated(EnumType.STRING)
	@Column(name = "availability_status", nullable = false)
	@Builder.Default
	private PlayerAvailabilityStatus availabilityStatus = PlayerAvailabilityStatus.AVAILABLE;

	public void updateAvailabilityStatus(PlayerAvailabilityStatus availabilityStatus) {
		this.availabilityStatus = availabilityStatus == null
				? PlayerAvailabilityStatus.AVAILABLE
				: availabilityStatus;
	}
}
