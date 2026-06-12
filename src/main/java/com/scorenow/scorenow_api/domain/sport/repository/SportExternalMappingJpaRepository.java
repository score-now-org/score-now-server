package com.scorenow.scorenow_api.domain.sport.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportExternalMappingJpaRepository extends JpaRepository<SportExternalMapping, Long> {
    Optional<SportExternalMapping> findByProviderAndApiSportId(DataOrigin provider, String apiSportId);
}
