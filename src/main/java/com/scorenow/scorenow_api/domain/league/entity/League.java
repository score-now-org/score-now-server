package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Setter;

@Entity
@Table(name = "leagues",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_leagues",
			columnNames = {"sport_id", "e_name"})})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class League extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sport_id")
	private Long sportId;

	@Column(name = "k_name")
	private String kName;

	@Column(name = "e_name")
	private String eName;

	@Column(name = "s_name")
	private String sName; // 숏네임

	private String cc;
}