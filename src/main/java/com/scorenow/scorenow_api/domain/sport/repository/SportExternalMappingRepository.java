package com.scorenow.scorenow_api.domain.sport.repository;

import java.util.Optional;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface SportExternalMappingRepository {
	Optional<SportExternalMapping> findByProviderAndExternalSportId(ApiProvider provider, String apiSportId);

	SportExternalMapping save(SportExternalMapping sportExternalMapping);
}
