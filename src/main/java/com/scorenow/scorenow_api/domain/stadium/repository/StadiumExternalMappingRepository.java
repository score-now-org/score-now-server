package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import java.util.Optional;

public interface StadiumExternalMappingRepository {
    Optional<StadiumExternalMapping> findByExternalInfo(DataOrigin provider, String apiSportId, String apiStadiumId);

    StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping);
}
