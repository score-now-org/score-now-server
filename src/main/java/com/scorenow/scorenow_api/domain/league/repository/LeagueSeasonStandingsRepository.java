package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeagueSeasonStandingsRepository extends JpaRepository<LeagueSeasonStandings, Long> {
    boolean existsByLeagueSeasonId(Long leagueSeasonId);

    Optional<LeagueSeasonStandings> findByLeagueSeasonId(Long leagueSeasonId);

    @Query(value = """
            select lss
            from LeagueSeasonStandings lss
            join fetch lss.leagueSeason ls
            join fetch ls.league l
            where lss.leagueSeasonId = :leagueSeasonId
              and ls.isActive = true
            """)
    Optional<LeagueSeasonStandings> findByLeagueSeasonIdWithSeasonAndLeague(
            @Param("leagueSeasonId") Long leagueSeasonId);

    @Query(value = """
            select lss
            from LeagueSeasonStandings lss
            join fetch lss.leagueSeason ls
            join fetch ls.league l
            where (:leagueId is null or :leagueId = l.id)
              and (
                   :leagueName is null
                   or l.kName like concat('%', :leagueName, '%')
                   or lower(l.eName) like lower(concat('%', :leagueName, '%'))
              )
              and ls.isActive = true
            """,
            countQuery = """
            select count(s)
            from LeagueSeasonStandings s
            join s.leagueSeason ls
            join ls.league l
            where (:leagueId is null or l.id = :leagueId)
              and (
                   :leagueName is null
                   or l.kName like concat('%', :leagueName, '%')
                   or lower(l.eName) like lower(concat('%', :leagueName, '%'))
              )
              and ls.isActive = true
            """
    )
    Page<LeagueSeasonStandings> searchLeagueSeasonStandings(
            @Param("leagueId") Long leagueId,
            @Param("leagueName") String leagueName,
            Pageable pageable);

    @Query(value = """
            select lss.leagueSeasonId
            from LeagueSeasonStandings lss
            join lss.leagueSeason ls
            where ls.isCurrent = true
              and ls.isActive = true
              and lss.standingsType = :standingsType
            """)
    List<Long> findCurrentLeagueSeasonIdsByStandingType(
            @Param("standingsType") LeagueSeasonStandingsType standingsType);
}
