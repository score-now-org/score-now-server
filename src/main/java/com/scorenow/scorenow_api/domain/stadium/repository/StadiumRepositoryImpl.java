package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
@RequiredArgsConstructor
public class StadiumRepositoryImpl implements StadiumRepository {

    private final StadiumJpaRepository stadiumJpaRepository;

    @Override
    public List<Stadium> searchStadiums(Long stadiumId, String name) {
        return stadiumJpaRepository.searchStadiums(stadiumId, name);
    }

    @Override
    public Page<Stadium> searchStadiums(Long stadiumId, String name, Pageable pageable) {
        return stadiumJpaRepository.searchStadiumsPage(stadiumId, name, null, pageable);
    }

    @Override
    public Page<Stadium> searchStadiums(Long stadiumId, String name, Long sportId, Pageable pageable) {
        return stadiumJpaRepository.searchStadiumsPage(stadiumId, name, sportId, pageable);
    }

    @Override
    public Stadium save(Stadium stadium) {
        return stadiumJpaRepository.save(stadium);
    }

    @Override
    public Optional<Stadium> findById(Long stadiumId) {
        return stadiumJpaRepository.findById(stadiumId);
    }

    @Override
    public List<Stadium> findAllByIds(Collection<Long> stadiumIds) {
        return stadiumJpaRepository.findAllById(stadiumIds);
    }

    @Override
    public Optional<Stadium> findActiveById(Long stadiumId) {
        return stadiumJpaRepository.findByIdAndIsActiveTrue(stadiumId);
    }
}
