package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeagueSeasonRepository extends JpaRepository<LeagueSeason, Long> {
    boolean existsByLeagueIdAndSeasonName(Long leagueId, String seasonName);

    @Query(value = """
            select ls
            from LeagueSeason ls
            where ls.isCurrent = true
              and ls.leagueId = :leagueId
              and ls.isActive = true
            """)
    List<LeagueSeason> findCurrentSeasonByLeagueId(@Param("leagueId") Long leagueId);

    Optional<LeagueSeason> findByIdAndIsActiveTrue(Long id);

    List<LeagueSeason> findAllByLeagueIdAndIsActiveTrue(Long leagueId);

    @Query(value = """
            select new com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse(
                ls.leagueId,
                coalesce(l.kName, l.eName),
                ls.id,
                ls.seasonName,
                ls.seasonStartAt,
                ls.seasonEndAt,
                ls.isCurrent
            )
            from LeagueSeason ls
            join ls.league l
            where (:leagueId is null or ls.leagueId = :leagueId)
              and (
                    :leagueName is null
                    or l.kName like concat('%',:leagueName,'%')
                    or lower(l.eName) like lower(concat('%',:leagueName,'%'))
              )
              and ls.isActive = true
            order by ls.isCurrent desc, ls.seasonStartAt desc
            """,
           countQuery = """
            select count(ls)
            from LeagueSeason ls
            join ls.league l
            where (:leagueId is null or ls.leagueId = :leagueId)
              and (
                    :leagueName is null
                    or l.kName like concat('%',:leagueName,'%')
                    or lower(l.eName) like lower(concat('%',:leagueName,'%'))
              )
              and ls.isActive = true
            """)
    Page<AdminLeagueSeasonResponse> searchLeagueSeasons(
            @Param("leagueId") Long leagueId,
            @Param("leagueName") String leagueName,
            Pageable pageable);
}
