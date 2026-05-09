package com.scorenow.scorenow_api.domain.sport.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import java.util.Optional;

public interface SportExternalMappingRepository {
    Optional<SportExternalMapping> findByProviderAndApiSportId(ApiProvider provider, String apiSportId);

    SportExternalMapping save(SportExternalMapping sportExternalMapping);
}
