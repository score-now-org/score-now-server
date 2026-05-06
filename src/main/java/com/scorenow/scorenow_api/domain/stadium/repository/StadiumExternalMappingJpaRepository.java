package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StadiumExternalMappingJpaRepository extends JpaRepository<StadiumExternalMapping, Long> {
    @Query("select s from StadiumExternalMapping s " +
            "where s.provider = :provider " +
            "and s.externalSportId = :externalSportId " +
            "and s.externalStadiumId = :externalStadiumId")
    Optional<StadiumExternalMapping> findByExternalInfo(@NonNull ExternalProvider provider, @NonNull String externalSportId, @NonNull String externalStadiumId);
}
