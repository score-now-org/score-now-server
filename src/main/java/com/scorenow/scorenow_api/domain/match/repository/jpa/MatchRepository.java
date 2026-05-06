package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;

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
				m.externalMatchId,
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
				on hem.team.id = h.id
				and hem.provider = m.provider
			join TeamExternalMapping aem
				on aem.team.id = a.id
				and aem.provider = m.provider
			where m.statusCode = :status
			  and m.isActive = true
		""")
	List<InplayScanDto> findInplayMatchDtos(@Param("status") MatchStatus status);

	boolean existsByIdAndStatusCode(Long id, MatchStatus statusCode);
}