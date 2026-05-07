package com.scorenow.scorenow_api.domain.sport.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SportExternalMappingRepositoryImpl implements SportExternalMappingRepository {

	private final SportExternalMappingJpaRepository jpaRepository;

	@Override
	public Optional<SportExternalMapping> findByProviderAndExternalSportId(ApiProvider provider,
		String externalSportId) {
		return jpaRepository.findByProviderAndExternalSportId(provider, externalSportId);
	}

	@Override
	public SportExternalMapping save(SportExternalMapping sportExternalMapping) {
		return jpaRepository.save(sportExternalMapping);
	}
}
