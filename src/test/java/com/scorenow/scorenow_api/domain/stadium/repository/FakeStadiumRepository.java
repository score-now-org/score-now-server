package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeStadiumRepository implements StadiumRepository {

    private final Map<Long, Stadium> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public List<Stadium> findAll() {
        return database.values().stream().toList();
    }

    @Override
    public List<Stadium> findByNameContainingIgnoreCase(String stadiumName) {
        List<Stadium> stadiums = database.values().stream().toList();
        return stadiums.stream()
                .filter(stadium -> stadiumName.equals(stadium.getName()))
                .toList();
    }

    @Override
    public Stadium save(Stadium stadium) {
        long id = idGenerator.getAndIncrement();
        database.put(id, stadium);

        return database.get(id);
    }

    @Override
    public Optional<Stadium> findById(Long stadiumId) {
        return Optional.of(database.get(stadiumId));
    }

    public int getSize() {
        return database.size();
    }
}
