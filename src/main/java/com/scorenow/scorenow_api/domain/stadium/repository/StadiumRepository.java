package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import java.util.List;
import java.util.Optional;

public interface StadiumRepository {
    List<Stadium> searchStadiums(Long stadiumId, String name);

    Stadium save(Stadium stadium);

    Optional<Stadium> findById(Long stadiumId);
}
