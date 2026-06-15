package com.scorenow.scorenow_api.domain.stadium.repository;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StadiumJpaRepository extends JpaRepository<Stadium, Long> {
    @Query("""
            select s
            from Stadium s
            where (:stadiumId is null or s.id = :stadiumId)
              and (:name is null or lower(s.name) like lower(concat('%', :name, '%')))
            """)
    List<Stadium> searchStadiums(
            @Param("stadiumId") Long stadiumId,
            @Param("name") String name
    );
}
