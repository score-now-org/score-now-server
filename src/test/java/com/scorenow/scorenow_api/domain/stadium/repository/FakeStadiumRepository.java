package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeStadiumRepository implements StadiumRepository {

    private final Map<Long, Stadium> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public List<Stadium> searchStadiums(Long stadiumId, String name) {
        return database.values().stream()
                .filter(stadium -> stadiumId == null || stadiumId.equals(stadium.getId()))
                .filter(stadium -> name == null || stadium.getName() != null && stadium.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    @Override
    public Stadium save(Stadium stadium) {
        long id = idGenerator.getAndIncrement();
        database.put(id, stadium);
        ReflectionTestUtils.setField(stadium, "id", id);
        return database.get(id);
    }

    @Override
    public Optional<Stadium> findById(Long stadiumId) {
        return Optional.ofNullable(database.get(stadiumId));
    }

    public int getSize() {
        return database.size();
    }
}
