package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

import java.util.Optional;

public interface StadiumExternalMappingRepository {
    Optional<StadiumExternalMapping> findByExternalInfo(ExternalProvider provider, String externalSportId, String externalStadiumId);

    StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping);
}
