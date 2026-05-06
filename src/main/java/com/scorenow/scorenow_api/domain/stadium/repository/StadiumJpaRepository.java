package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StadiumJpaRepository extends JpaRepository<Stadium, Long> {
    List<Stadium> findByNameContainingIgnoreCase(String name);
}
