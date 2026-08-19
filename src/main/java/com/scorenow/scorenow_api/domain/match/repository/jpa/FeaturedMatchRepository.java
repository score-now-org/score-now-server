package com.scorenow.scorenow_api.domain.match.repository.jpa;

import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatch;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeaturedMatchRepository extends JpaRepository<FeaturedMatch, Long> {
    boolean existsByMatchId(Long matchId);

    @Query("""
            select max(fm.displayOrder)
            from FeaturedMatch fm
            where fm.displayDate = :displayDate
              and fm.type = :type
            """)
    Optional<Integer> findMaxDisplayOrder(
            @Param("displayDate") LocalDate displayDate,
            @Param("type") FeaturedMatchType type);

    List<FeaturedMatch> findByDisplayDateAndTypeOrderByDisplayOrderAsc(LocalDate displayDate, FeaturedMatchType type);

    @Query("""
            select fm
            from FeaturedMatch fm
            join fetch fm.match m
            join fetch m.sport
            join fetch m.league
            join fetch m.homeTeam
            join fetch m.awayTeam
            where fm.displayDate = :displayDate
            order by fm.displayOrder asc
            """)
    List<FeaturedMatch> findByDisplayDateWithMatch(@Param("displayDate") LocalDate displayDate);

    @Query("""
            select fm
            from FeaturedMatch fm
            where fm.displayDate = :displayDate
              and fm.type = :type
              and fm.displayOrder > :deletedDisplayOrder
            order by fm.displayOrder asc
            """)
    List<FeaturedMatch> findMatchesAfterDisplayOrder(
            @Param("displayDate") LocalDate displayDate,
            @Param("type") FeaturedMatchType type,
            @Param("deletedDisplayOrder") Integer deletedDisplayOrder);

    @Query("""
            select fm.matchId
            from FeaturedMatch fm
            where fm.matchId in :matchIds
            """)
    List<Long> findRegisteredMatchIds(@Param("matchIds") List<Long> matchIds);

    Optional<FeaturedMatch> findByMatchId(Long matchId);
}
