package com.scorenow.scorenow_api.domain.sport.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import java.util.Optional;

public interface SportExternalMappingRepository {
    Optional<SportExternalMapping> findByProviderAndApiSportId(DataOrigin provider, String apiSportId);

    SportExternalMapping save(SportExternalMapping sportExternalMapping);
}
