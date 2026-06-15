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
    public List<Stadium> searchStadiums(Long stadiumId, String name) {
        return stadiumJpaRepository.searchStadiums(stadiumId, name);
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
