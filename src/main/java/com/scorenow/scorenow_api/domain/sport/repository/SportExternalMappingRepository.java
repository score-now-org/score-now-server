package com.scorenow.scorenow_api.domain.sport.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportExternalMappingRepository extends JpaRepository<SportExternalMapping, Long> {
    Optional<SportExternalMapping> findByProviderAndExternalSportId(ExternalProvider provider, String externalSportId);
}
