package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.external.common.ApiProvider;
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
				and hem.provider = m.provider
			join TeamExternalMapping aem
				on aem.internalTeamId = a.id
				and aem.provider = m.provider
			where m.statusCode = :status
			  and m.isActive = true
		""")
	List<InplayScanDto> findInplayMatchDtos(@Param("status") MatchStatus status);

    boolean existsByIdAndStatusCode(Long id, MatchStatus statusCode);

    @Query("select m from Match m " +
            "where m.provider = :provider " +
            "and m.apiMatchId = :apiMatchId")
    Optional<Match> findByExternalInfo(ApiProvider provider, String apiMatchId);
}
