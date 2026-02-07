package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.match.dto.InplayMatchDetectionDto;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;

public interface MatchRepository extends JpaRepository<Match, String> {
	List<Match> findByStatusCode(MatchStatus statusCode);

	@Query("""
			select new com.scorenow.scorenow_api.domain.match.dto.InplayMatchDetectionDto(
					m.id, h.id, h.eName, a.id, a.eName
			)
			from Match m
			join Team h on h.id = m.homeId
			join Team a on a.id = m.awayId
			where m.statusCode = :status and m.isActive = true
		""")
	List<InplayMatchDetectionDto> findInplayMatchDtos(@Param("status") MatchStatus status);
}
