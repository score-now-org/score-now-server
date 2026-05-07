package com.scorenow.scorenow_api.domain.stadium.repository;

import java.util.Optional;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface StadiumExternalMappingRepository {
	Optional<StadiumExternalMapping> findByExternalInfo(ApiProvider provider, String externalSportId,
		String externalStadiumId);

	StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping);
}
