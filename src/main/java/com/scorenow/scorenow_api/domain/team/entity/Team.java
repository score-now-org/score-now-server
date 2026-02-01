package com.scorenow.scorenow_api.domain.team.entity;

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
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {
	@Id
	private String id; // BETS + sportId + teamId
	private String type;    // 구분 (국가대표, 클럽)
	private String kName;
	private String eName;
	private String sName; // 숏네임
	private String sportId;
	private String cc; // 국가코드
	private String imageUrl;

	@Builder.Default
	private boolean isActive = true;

	public static String generateId(String sportId, String teamId) {
		return "BETS" + sportId + teamId;
	}

}