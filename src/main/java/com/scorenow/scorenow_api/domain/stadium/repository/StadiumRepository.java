package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public interface StadiumRepository {
    List<Stadium> searchStadiums(Long stadiumId, String name);

    default Page<Stadium> searchStadiums(Long stadiumId, String name, Pageable pageable) {
        List<Stadium> stadiums = searchStadiums(stadiumId, name);
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= stadiums.size()) {
            return new PageImpl<>(List.of(), pageable, stadiums.size());
        }
        int end = Math.min(start + pageable.getPageSize(), stadiums.size());
        return new PageImpl<>(stadiums.subList(start, end), pageable, stadiums.size());
    }

    default Page<Stadium> searchStadiums(Long stadiumId, String name, Long sportId, Pageable pageable) {
        List<Stadium> stadiums = searchStadiums(stadiumId, name).stream()
                .filter(stadium -> sportId == null || sportId.equals(stadium.getSportId()))
                .toList();
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= stadiums.size()) {
            return new PageImpl<>(List.of(), pageable, stadiums.size());
        }
        int end = Math.min(start + pageable.getPageSize(), stadiums.size());
        return new PageImpl<>(stadiums.subList(start, end), pageable, stadiums.size());
    }

    Stadium save(Stadium stadium);

    /**
     * 비활성 경기장도 포함하여 조회합니다. 기존 경기에 매핑된 경기장 정보 조회에 사용합니다.
     */
    Optional<Stadium> findById(Long stadiumId);

    /**
     * 활성 경기장만 조회합니다. 관리자 수정 및 신규 경기장 배정에 사용합니다.
     */
    default Optional<Stadium> findActiveById(Long stadiumId) {
        return findById(stadiumId).filter(Stadium::isActive);
    }
}
