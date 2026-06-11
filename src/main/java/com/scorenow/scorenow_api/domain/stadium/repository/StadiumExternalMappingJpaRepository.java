package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StadiumExternalMappingJpaRepository extends JpaRepository<StadiumExternalMapping, Long> {
    @Query("select s from StadiumExternalMapping s " +
            "where s.provider = :provider " +
            "and s.apiSportId = :apiSportId " +
            "and s.apiStadiumId = :apiStadiumId")
    Optional<StadiumExternalMapping> findByExternalInfo(@NonNull DataOrigin provider, @NonNull String apiSportId, @NonNull String apiStadiumId);
}
