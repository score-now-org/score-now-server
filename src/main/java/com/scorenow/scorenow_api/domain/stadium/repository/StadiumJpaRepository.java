package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StadiumJpaRepository extends JpaRepository<Stadium, Long> {
    @Query("""
            select s
            from Stadium s
            where (:stadiumId is null or s.id = :stadiumId)
              and s.isActive = true
              and (:name is null or lower(s.name) like lower(concat('%', :name, '%')))
            """)
    List<Stadium> searchStadiums(
            @Param("stadiumId") Long stadiumId,
            @Param("name") String name
    );

    @Query(value = """
            select s
            from Stadium s
            where (:stadiumId is null or s.id = :stadiumId)
              and s.isActive = true
              and (:name is null or lower(s.name) like lower(concat('%', :name, '%')))
              and (:sportId is null or s.sportId = :sportId)
            """,
            countQuery = """
            select count(s)
            from Stadium s
            where (:stadiumId is null or s.id = :stadiumId)
              and s.isActive = true
              and (:name is null or lower(s.name) like lower(concat('%', :name, '%')))
              and (:sportId is null or s.sportId = :sportId)
            """)
    Page<Stadium> searchStadiumsPage(
            @Param("stadiumId") Long stadiumId,
            @Param("name") String name,
            @Param("sportId") Long sportId,
            Pageable pageable
    );

    Optional<Stadium> findByIdAndIsActiveTrue(Long stadiumId);
}
