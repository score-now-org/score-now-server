package com.scorenow.scorenow_api.domain.player.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;

public record AdminPlayerResponse(
	Long id,
	Long sportId,
	String sportName,
	String kName,
	String eName,
	String cc,
	LocalDate birthdate,
	Integer height,
	boolean teaguk,
	List<TeamDetailResponse> teams
) {
	public record TeamDetailResponse(
		Long teamId,
		String teamName,
		Long leagueId,
		String leagueName,
		String position,
		String shirtNumber,
		boolean squadOn
	) {
		public static TeamDetailResponse from(PlayerTeamDetail detail) {
			return new TeamDetailResponse(
				detail.getTeam().getId(),
				detail.getTeam().resolveTeamName(),
				detail.getLeague().getId(),
				detail.getLeague().resolveLeagueName(),
				detail.getPosition(),
				detail.getShirtNumber(),
				detail.isSquadOn()
			);
		}
	}

	public static AdminPlayerResponse from(
		Player player,
		List<PlayerTeamDetail> details
	) {
		return new AdminPlayerResponse(
			player.getId(),
			player.getSport().getId(),
			player.getSport().resolveSportName(),
			player.getKName(),
			player.getEName(),
			player.getCc(),
			player.getBirthdate(),
			player.getHeight(),
			player.isTeaguk(),
			details.stream()
				.map(TeamDetailResponse::from)
				.toList()
		);
	}
}
