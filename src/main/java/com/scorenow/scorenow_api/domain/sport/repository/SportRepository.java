package com.scorenow.scorenow_api.domain.sport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;

public interface SportRepository extends JpaRepository<Sport, String> {
}
