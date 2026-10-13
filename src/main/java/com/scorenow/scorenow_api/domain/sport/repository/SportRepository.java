package com.scorenow.scorenow_api.domain.sport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Long> {
    List<Sport> findAllByOrderByIdAsc();
}
