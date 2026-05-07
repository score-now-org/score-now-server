package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class StadiumCacheKey {
	@NonNull
	private final ApiProvider provider;
	@NonNull
	private final String externalSportId;
	@NonNull
	private final String externalStadiumId;
}
