package com.scorenow.scorenow_api.domain.sport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface SportExternalMappingJpaRepository extends JpaRepository<SportExternalMapping, Long> {
	Optional<SportExternalMapping> findByProviderAndExternalSportId(ApiProvider provider, String externalSportId);
}
