package com.scorenow.scorenow_api.domain.match.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

@Component
public class MatchLineupMapper {

	public LineupSide toSide(
		BetsLineupResponse.LineupSide betsLineupSide,
		Long teamId,
		String apiTeamId,
		String teamEname
	) {
		if (betsLineupSide == null) {

			return LineupSide.builder()
				.teamId(teamId)
				.apiTeamId(apiTeamId)
				.teamEname(teamEname)
				.formation(null)
				.startingLineup(new ArrayList<>())
				.substitutes(new ArrayList<>())
				.build();
		}

		return LineupSide.builder()
			.teamId(teamId)
			.apiTeamId(apiTeamId)
			.teamEname(teamEname)
			.formation(betsLineupSide.getFormation())
			.startingLineup(toPlayers(betsLineupSide.getStartinglineup()))
			.substitutes(toPlayers(betsLineupSide.getSubstitutes()))
			.build();
	}

	public List<LineupPlayer> toPlayers(List<BetsLineupResponse.LineupPlayer> betsPlayers) {
		if (betsPlayers == null)
			return new ArrayList<>();

		List<LineupPlayer> list = new ArrayList<>(betsPlayers.size());
		for (BetsLineupResponse.LineupPlayer betsPlayer : betsPlayers) {
			list.add(toPlayer(betsPlayer));
		}
		return list;
	}

	public LineupPlayer toPlayer(BetsLineupResponse.LineupPlayer betsPlayer) {
		String playerApiId = null;
		String eName = null;
		String shirtNumber = null;

		if (betsPlayer != null) {
			shirtNumber = betsPlayer.getShirtnumber();

			if (betsPlayer.getPlayer() != null) {
				playerApiId = betsPlayer.getPlayer().getId();
				eName = betsPlayer.getPlayer().getName();
			}
		}

		return LineupPlayer.builder()
			.playerId(null)
			.apiPlayerId(playerApiId)
			.eName(eName)
			.shirtNumber(shirtNumber)
			.position(mapPosition(betsPlayer != null ? betsPlayer.getPos() : null))
			.goals(0)
			.build();
	}

	private String mapPosition(String pos) {
		if (pos == null)
			return null;

		return switch (pos) {
			case "Guard" -> "GK";
			case "Defender" -> "DF";
			case "Midfielder" -> "MF";
			case "Forward" -> "FW";
			default -> "SUB";
		};
	}
}
