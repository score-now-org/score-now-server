package com.scorenow.scorenow_api.domain.team.entity;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Team extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String type;    // 구분 (국가대표, 클럽)
	private String kName;
	private String eName;
	private String sName; // 숏네임

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "sport_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Sport sport;

	private String cc; // 국가코드
	private String imageUrl;

}