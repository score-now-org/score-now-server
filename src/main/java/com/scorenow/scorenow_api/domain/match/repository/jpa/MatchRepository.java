package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.external.common.ExternalProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;

public interface MatchRepository extends JpaRepository<Match, Long>, MatchRepositoryCustom {
    List<Match> findByStatusCode(MatchStatus statusCode);

    @Query("""
            	select new com.scorenow.scorenow_api.domain.match.dto.InplayScanDto(
            			m.id, m.sportId, h.id, h.eName, a.id, a.eName
            	)
            	from Match m
            	join Team h on h.id = m.homeId
            	join Team a on a.id = m.awayId
            	where m.statusCode = :status and m.isActive = true
            """)
    List<InplayScanDto> findInplayMatchDtos(@Param("status") MatchStatus status);

    boolean existsByIdAndStatusCode(Long id, MatchStatus statusCode);

    @Query("select m from Match m " +
            "where m.provider = :provider " +
            "and m.externalMatchId = :externalMatchId")
    Optional<Match> findByExternalInfo(ExternalProvider provider, String externalMatchId);
}
