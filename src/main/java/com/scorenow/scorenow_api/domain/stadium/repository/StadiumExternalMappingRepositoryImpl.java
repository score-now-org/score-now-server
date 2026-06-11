package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StadiumExternalMappingRepositoryImpl implements StadiumExternalMappingRepository {
    private final StadiumExternalMappingJpaRepository jpaRepository;

    @Override
    public Optional<StadiumExternalMapping> findByExternalInfo(@NonNull DataOrigin provider, @NonNull String apiSportId, @NonNull String apiStadiumId) {
        return jpaRepository.findByExternalInfo(provider, apiSportId, apiStadiumId);
    }

    @Override
    public StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping) {
        return jpaRepository.save(stadiumExternalMapping);
    }
}
