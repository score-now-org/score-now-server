package com.scorenow.scorenow_api.domain.team.entity;

import com.scorenow.scorenow_api.external.common.ExternalProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_external_mappings",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_external_stadium_id_sport_id",   // TODO: 유니크키 제약조건 이름 변경
			columnNames = {"provider", "external_league_id", "internal_league_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class TeamExternalMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "provider", nullable = false)
	@Enumerated(EnumType.STRING)
	private ExternalProvider provider;

	@Column(name = "external_team_id", nullable = false)
	private String externalTeamId;   // 외부API 를 통해 전달받은 팀 ID

	@Column(name = "internal_team_id", nullable = false)
	private Long internalTeamId;     // 시스템 내부에서 채번한 팀 ID

	public static TeamExternalMapping of(ExternalProvider provider, String externalTeamId, Long internalTeamId) {
		return new TeamExternalMapping(null, provider, externalTeamId, internalTeamId);
	}
}
