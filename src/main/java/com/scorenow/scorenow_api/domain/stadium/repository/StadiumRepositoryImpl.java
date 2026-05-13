package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StadiumRepositoryImpl implements StadiumRepository {

    private final StadiumJpaRepository stadiumJpaRepository;

    @Override
    public List<Stadium> findAll() {
        return stadiumJpaRepository.findAll();
    }

    @Override
    public List<Stadium> findByNameContainingIgnoreCase(String stadiumName) {
        return stadiumJpaRepository.findByNameContainingIgnoreCase(stadiumName);
    }

    @Override
    public Stadium save(Stadium stadium) {
        return stadiumJpaRepository.save(stadium);
    }

    @Override
    public Optional<Stadium> findById(Long stadiumId) {
        return stadiumJpaRepository.findById(stadiumId);
    }
}
