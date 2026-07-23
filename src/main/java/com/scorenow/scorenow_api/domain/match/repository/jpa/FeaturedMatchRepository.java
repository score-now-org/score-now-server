package com.scorenow.scorenow_api.domain.match.repository.jpa;

import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchResponse;
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
            select new com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchResponse(
                fm.id,
                fm.matchId,
                coalesce(l.kName, l.eName),
                coalesce(ht.kName, ht.eName),
                coalesce(at.kName, at.eName),
                m.startAt,
                fm.type,
                fm.displayOrder
            )
            from FeaturedMatch fm
            join fm.match m
            join m.league l
            join m.homeTeam ht
            join m.awayTeam at
            where fm.displayDate = :displayDate
            order by fm.displayOrder asc
            """)
    List<FeaturedMatchResponse> findByDisplayDateWithLeague(@Param("displayDate") LocalDate displayDate);

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
