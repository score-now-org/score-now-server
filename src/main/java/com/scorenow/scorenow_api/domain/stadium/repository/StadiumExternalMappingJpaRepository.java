package com.scorenow.scorenow_api.domain.stadium.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.NonNull;

public interface StadiumExternalMappingJpaRepository extends JpaRepository<StadiumExternalMapping, Long> {
	@Query("select s from StadiumExternalMapping s " +
		"where s.provider = :provider " +
		"and s.apiSportId = :externalSportId " +
		"and s.apiStadiumId = :externalStadiumId")
	Optional<StadiumExternalMapping> findByExternalInfo(@NonNull ApiProvider provider, @NonNull String externalSportId,
		@NonNull String externalStadiumId);
}
