package com.scorenow.scorenow_api.domain.player.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;

public record AdminPlayerResponse(
	Long id,
	String provider,
	String apiPlayerId,
	Long sportId,
	String sportName,
	String kName,
	String eName,
	String cc,
	LocalDate birthdate,
	Integer height,
	boolean taegeuk,
	List<TeamDetailResponse> teams
) {
	public record TeamDetailResponse(
		Long playerTeamDetailId,
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
				detail.getId(),
				detail.getTeam().getId(),
				detail.getTeam().resolveTeamName(),
				detail.getLeague() != null
					? detail.getLeague().getId()
					: null,
				detail.getLeague() != null
					? detail.getLeague().resolveLeagueName()
					: null,
				detail.getPosition(),
				detail.getShirtNumber(),
				detail.isSquadOn()
			);
		}
	}

	public static AdminPlayerResponse from(
		Player player,
		List<PlayerTeamDetail> details,
		PlayerExternalMapping mapping
	) {
		return new AdminPlayerResponse(
			player.getId(),
			mapping != null && mapping.getProvider() != null
				? mapping.getProvider().name()
				: null,
			mapping != null
				? mapping.getApiPlayerId()
				: null,
			player.getSport().getId(),
			player.getSport().resolveSportName(),
			player.getKName(),
			player.getEName(),
			player.getCc(),
			player.getBirthdate(),
			player.getHeight(),
			player.isTaegeuk(),
			details.stream()
				.map(TeamDetailResponse::from)
				.toList()
		);
	}
}
