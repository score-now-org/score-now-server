package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeSportExternalMappingRepository implements SportExternalMappingRepository {

    private final Map<Long, SportExternalMapping> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public SportExternalMapping save(SportExternalMapping sportExternalMapping) {
        Long id = idGenerator.getAndIncrement();
        database.put(id, sportExternalMapping);
        return database.get(id);
    }

    @Override
    public Optional<SportExternalMapping> findByProviderAndApiSportId(DataOrigin provider, String apiSportId) {
        return database.values().stream()
                .filter(o -> provider.equals(o.getProvider()))
                .filter(o -> apiSportId.equals(o.getApiSportId()))
                .findAny();
    }
}
