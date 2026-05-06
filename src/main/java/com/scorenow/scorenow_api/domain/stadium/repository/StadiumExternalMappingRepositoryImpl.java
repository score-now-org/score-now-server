package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StadiumExternalMappingRepositoryImpl implements StadiumExternalMappingRepository {
    private final StadiumExternalMappingJpaRepository jpaRepository;

    @Override
    public Optional<StadiumExternalMapping> findByExternalInfo(@NonNull ExternalProvider provider, @NonNull String externalSportId, @NonNull String externalStadiumId) {
        return jpaRepository.findByExternalInfo(provider, externalSportId, externalStadiumId);
    }

    @Override
    public StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping) {
        return jpaRepository.save(stadiumExternalMapping);
    }
}
