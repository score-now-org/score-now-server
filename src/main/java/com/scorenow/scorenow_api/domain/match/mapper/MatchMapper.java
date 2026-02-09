package com.scorenow.scorenow_api.domain.match.mapper;

import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.team.entity.Team;

@Component
public class MatchMapper {

	public MatchListResponse toResponse(Match match) {
		return MatchListResponse.builder()
			.id(match.getId())
			.sportId(match.getSportId())
			.sportName(getSportName(match.getSport()))
			.leagueId(match.getLeagueId())
			.leagueName(getLeagueName(match.getLeague()))
			.matchType(match.getMatchType())
			.startAt(match.getStartAt())
			.statusCode(match.getStatusCode().name())
			.statusName(match.getStatusCode().getDescription())
			.homeId(match.getHomeId())
			.homeName(getTeamName(match.getHomeTeam()))
			.homeImageUrl(getTeamImageUrl(match.getHomeTeam()))
			.homeScore(match.getHomeScore())
			.awayId(match.getAwayId())
			.awayName(getTeamName(match.getAwayTeam()))
			.awayImageUrl(getTeamImageUrl(match.getAwayTeam()))
			.awayScore(match.getAwayScore())
			.isManual(match.isManual())
			.isActive(match.isActive())
			.build();
	}

	public Match toEntity(MatchCreateRequest request) {
		return Match.builder()
			.id(Match.generateManualId(request.getSportId()))
			.sportId(request.getSportId())
			.leagueId(request.getLeagueId())
			.homeId(request.getHomeId())
			.awayId(request.getAwayId())
			.startAt(request.getStartAt())
			.statusCode(MatchStatus.NOT_STARTED)
			.matchType("M")
			.isManual(true)
			.isActive(false)
			.build();
	}

	private String generateMatchId(String sportId, boolean isManual){
		if(isManual){
			return Match.generateManualId(sportId);
		}
		return Match.generateManualId(sportId);
	}

	private String getSportName(Sport sport) {
		return sport != null ? sport.getEName() : "";
	}

	private String getLeagueName(League league) {
		return league != null ? league.getEName() : "";
	}

	private String getTeamName(Team team) {
		return team != null ? team.getEName() : "";
	}

	private String getTeamImageUrl(Team team) {
		return team != null ? team.getImageUrl() : null;
	}
}
