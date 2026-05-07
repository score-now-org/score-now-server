package com.scorenow.scorenow_api.domain.sport.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stadium_external_mappings",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_external_stadium_id_sport_id",   // TODO: 여기 이름 바꾸기 나중에
			columnNames = {"provider", "api_sport_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SportExternalMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "provider", nullable = false)
	private ApiProvider provider;

	@Column(name = "api_sport_id", nullable = false)
	private String apiSportId;     // 외부API 를 통해 전달받은 종목 ID

	@Column(name = "internal_sport_id", nullable = false)
	private Long internalSportId;     // 시스템 내부에서 채번한 경기장 ID
}
