package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "league_external_mappings",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_league_external_mappings",
			columnNames = {"provider", "api_league_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class LeagueExternalMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false)
	private ApiProvider provider;

	@Column(name = "api_league_id", nullable = false)
	private String apiLeagueId;   // 외부API 를 통해 전달받은 리그 ID

	@Column(name = "internal_league_id", nullable = false)
	private Long internalLeagueId;     // 시스템 내부에서 채번한 리그 ID

	public static LeagueExternalMapping of(ApiProvider provider, String apiLeagueId, Long internalLeagueId) {
		return new LeagueExternalMapping(null, provider, apiLeagueId, internalLeagueId);
	}
}
