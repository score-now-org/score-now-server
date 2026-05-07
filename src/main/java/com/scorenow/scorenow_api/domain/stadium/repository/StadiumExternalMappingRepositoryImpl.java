package com.scorenow.scorenow_api.domain.stadium.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StadiumExternalMappingRepositoryImpl implements StadiumExternalMappingRepository {
	private final StadiumExternalMappingJpaRepository jpaRepository;

	@Override
	public Optional<StadiumExternalMapping> findByExternalInfo(@NonNull ApiProvider
		provider, @NonNull String externalSportId, @NonNull String externalStadiumId) {
		return jpaRepository.findByExternalInfo(provider, externalSportId, externalStadiumId);
	}

	@Override
	public StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping) {
		return jpaRepository.save(stadiumExternalMapping);
	}
}
