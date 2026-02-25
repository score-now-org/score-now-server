package com.scorenow.scorenow_api.domain.sport.entity;

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
@Table(name = "sports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sport extends BaseEntity {
	@Id
	private String id;
	private String kName;
	private String eName;

	@Builder.Default
	private boolean isActive = true;

	public static String generateSportId(String sportId) {
		return "BETS" + sportId;
	}
}