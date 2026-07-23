package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

public interface MatchRepository extends JpaRepository<Match, Long>, MatchRepositoryCustom {

    List<Match> findByStatusCode(MatchStatus statusCode);

    @Query("""
            	select new com.scorenow.scorenow_api.domain.match.dto.InplayScanDto(
            		m.id,
            		m.apiMatchId,
            		m.sportId,
            		h.id,
            		h.eName,
            		hem.apiTeamId,
            		a.id,
            		a.eName,
            		aem.apiTeamId
            	)
            	from Match m
            	join Team h on h.id = m.homeId
            	join Team a on a.id = m.awayId
            	join TeamExternalMapping hem
            		on hem.internalTeamId = h.id
            		and hem.provider = m.dataOrigin
            	join TeamExternalMapping aem
            		on aem.internalTeamId = a.id
            		and aem.provider = m.dataOrigin
            	where m.statusCode = :status
            	  and m.isActive = true
            """)
    List<InplayScanDto> findInplayMatchDtos(@Param("status") MatchStatus status);

    boolean existsByIdAndStatusCode(Long id, MatchStatus statusCode);

    @Query("""
            	select m
            	from Match m
            	where m.dataOrigin = :dataOrigin
            	  and m.apiMatchId = :apiMatchId
                  and m.isManual = false
            """)
    Optional<Match> findByExternalInfo(
            @Param("dataOrigin") DataOrigin dataOrigin,
            @Param("apiMatchId") String apiMatchId
    );

    @Query("""
            	 select new com.scorenow.scorenow_api.domain.match.dto.MatchCandidate(
            		m.id,
            		m.sportId,
            		m.dataOrigin,
            		m.apiMatchId,
            		sem.apiSportId,
                    m.startAt
            	)
            	  from Match m
            	  join SportExternalMapping sem
            		on sem.internalSportId = m.sportId
            	  where m.statusCode = :matchStatus
            		and m.startAt between :startAt and :targetTime
            		and m.isManual = false
            """)
    List<MatchCandidate> findMatchesStartingWithin(MatchStatus matchStatus, LocalDateTime startAt, LocalDateTime targetTime);

    @Modifying(clearAutomatically = true)
    @Query("update Match m set m.statusCode = :status, m.updatedAt = CURRENT_TIMESTAMP where m.id IN :matchIds")
    int updateStatusBulk(List<Long> matchIds, MatchStatus status);

    List<Match> findByStatusCodeAndIsManualFalse(MatchStatus matchStatus);

    @Query("""
            select distinct m
            from Match m
            left join fetch m.league
            left join fetch m.homeTeam
            left join fetch m.awayTeam
            where m.isActive = true
              and m.startAt >= :startAt
              and m.startAt < :endAt
              and (:sportId is null or m.sportId = :sportId)
              and (:leagueId is null or m.leagueId = :leagueId)
              and m.statusCode in :statuses
            order by
              case
                when m.statusCode in :inPlayStatuses then 0
                when m.statusCode in :scheduledStatuses then 1
                when m.statusCode in :endedStatuses then 2
                else 3
              end asc,
              m.startAt asc
            """)
    List<Match> findAppMatches(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("sportId") Long sportId,
            @Param("leagueId") Long leagueId,
            @Param("statuses") List<MatchStatus> statuses,
            @Param("inPlayStatuses") List<MatchStatus> inPlayStatuses,
            @Param("scheduledStatuses") List<MatchStatus> scheduledStatuses,
            @Param("endedStatuses") List<MatchStatus> endedStatuses
    );

    @Query("""
            select m
            from Match m
            join fetch m.league l
            join fetch m.homeTeam ht
            join fetch m.awayTeam at
            where m.startAt >= :start and m.startAt < :endExclusive
              and m.isActive = true
            """)
    List<Match> findMatchesByDateRange(LocalDateTime start, LocalDateTime endExclusive);

}
