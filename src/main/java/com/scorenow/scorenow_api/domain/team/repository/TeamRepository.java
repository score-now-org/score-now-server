package com.scorenow.scorenow_api.domain.team.repository;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.team.entity.Team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO teams (id, sport_id, type, k_name, e_name, s_name, cc, image_url, data_origin, is_active, created_at, updated_at)
            VALUES (:id, :sportId, :type, :kName, :eName, :sName, :cc, :imageUrl, :dataOrigin, true, NOW(), NOW())
            """, nativeQuery = true)
    void insertIgnore(
            @Param("id") Long id,
            @Param("sportId") Long sportId,
            @Param("type") String type,
            @Param("kName") String kName,
            @Param("eName") String eName,
            @Param("sName") String sName,
            @Param("cc") String cc,
            @Param("imageUrl") String imageUrl,
            @Param("dataOrigin") String dataOrigin
    );

    @Query(value = """
            select t
            from Team t
            left join fetch t.sport
            where (:teamId is null or t.id = :teamId)
              and t.isActive = true
              and (:keyword is null
                   or lower(t.kName) like lower(concat('%', :keyword, '%'))
                   or lower(t.eName) like lower(concat('%', :keyword, '%'))
                   or lower(t.sName) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(t)
            from Team t
            where (:teamId is null or t.id = :teamId)
              and t.isActive = true
              and (:keyword is null
                   or lower(t.kName) like lower(concat('%', :keyword, '%'))
                   or lower(t.eName) like lower(concat('%', :keyword, '%'))
                   or lower(t.sName) like lower(concat('%', :keyword, '%')))
            """)
    Page<Team> searchTeams(
            @Param("teamId") Long teamId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select t
            from Team t
            left join fetch t.sport
            where t.isActive = true
              and t.sport.id = :sportId
              and (lower(t.kName) like lower(concat('%', :keyword, '%'))
                   or lower(t.eName) like lower(concat('%', :keyword, '%'))
                   or lower(t.sName) like lower(concat('%', :keyword, '%')))
            order by
              case
                when t.kName is not null and t.kName <> '' then t.kName
                when t.eName is not null and t.eName <> '' then t.eName
                when t.sName is not null and t.sName <> '' then t.sName
                else ''
              end asc,
              t.id asc
            """)
    List<Team> searchMatchTeamCandidates(
            @Param("sportId") Long sportId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<Team> findByIdAndIsActiveTrue(Long id);

    boolean existsByIdAndIsActiveTrue(Long id);
}
