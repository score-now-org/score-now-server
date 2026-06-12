package com.scorenow.scorenow_api.domain.sport.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SportExternalMappingRepositoryImpl implements SportExternalMappingRepository {

    private final SportExternalMappingJpaRepository jpaRepository;

    @Override
    public Optional<SportExternalMapping> findByProviderAndApiSportId(DataOrigin provider, String apiSportId) {
        return jpaRepository.findByProviderAndApiSportId(provider, apiSportId);
    }

    @Override
    public SportExternalMapping save(SportExternalMapping sportExternalMapping) {
        return jpaRepository.save(sportExternalMapping);
    }
}
