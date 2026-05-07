package com.scorenow.scorenow_api.domain.stadium.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public class FakeStadiumExternalMappingRepository implements StadiumExternalMappingRepository {

	private final Map<Long, StadiumExternalMapping> database = new HashMap<>();
	private final AtomicLong idGenerator = new AtomicLong(0);

	@Override
	public StadiumExternalMapping save(StadiumExternalMapping stadiumExternalMapping) {
		long id = idGenerator.getAndIncrement();
		database.put(id, stadiumExternalMapping);

		return database.get(id);
	}

	@Override
	public Optional<StadiumExternalMapping> findByExternalInfo(ApiProvider provider, String apiSportId,
		String apiStadiumId) {
		return database.values().stream()
			.filter(o -> provider.equals(o.getProvider()))
			.filter(o -> apiSportId.equals(o.getApiSportId()))
			.filter(o -> apiStadiumId.equals(o.getApiStadiumId()))
			.findAny();
	}
}
