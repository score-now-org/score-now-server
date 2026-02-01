package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leagues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class League extends BaseEntity {
	@Id
	private String id; // BETS + sportId + leagueId
	private String sportId;
	private String kName;
	private String eName;
	private String sName; // 숏네임
	private String cc;

	@Builder.Default
	private boolean isActive = true;

	public static String generateLeagueId(String sportId, String leagueId) {
		return "BETS" + sportId + leagueId;
	}
}