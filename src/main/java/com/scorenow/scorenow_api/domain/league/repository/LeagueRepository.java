package com.scorenow.scorenow_api.domain.league.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.league.entity.League;

public interface LeagueRepository extends JpaRepository<League, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO leagues (id, sport_id, k_name, e_name, s_name, cc, is_active, created_at, updated_at)
            VALUES (:id, :sportId, :kName, :eName, :sName, :cc, true, NOW(), NOW())
            """, nativeQuery = true)
    void insertIgnore(
            @Param("id") Long id,
            @Param("sportId") Long sportId,
            @Param("kName") String kName,
            @Param("eName") String eName,
            @Param("sName") String sName,
            @Param("cc") String cc
    );

    @Query("""
            select l
            from League l
            left join fetch l.sport
            where (:leagueId is null or l.id = :leagueId)
              and (
                    :keyword is null
                    or l.kName like concat('%', :keyword, '%')
                    or lower(l.eName) like lower(concat('%', :keyword, '%'))
                    or lower(l.sName) like lower(concat('%', :keyword, '%'))
              )
            order by
              case
                when l.kName is not null and l.kName <> '' then l.kName
                when l.eName is not null and l.eName <> '' then l.eName
                when l.sName is not null and l.sName <> '' then l.sName
                else ''
              end asc,
              l.id asc
            """)
    List<League> searchLeagues(
            @Param("leagueId") Long leagueId,
            @Param("keyword") String keyword
    );

    @Query("""
            select l
            from League l
            left join fetch l.sport
            where l.id = :leagueId
            """)
    Optional<League> findByIdWithSport(@Param("leagueId") Long leagueId);
}
