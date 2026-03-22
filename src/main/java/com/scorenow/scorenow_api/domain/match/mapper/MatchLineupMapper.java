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
		String teamId,
		String teamEname
	) {
		if (betsLineupSide == null) {

			return LineupSide.builder()
				.teamId(teamId)
				.teamEname(teamEname)
				.formation(null)
				.startingLineup(new ArrayList<>())
				.substitutes(new ArrayList<>())
				.build();
		}

		return LineupSide.builder()
			.teamId(teamId)
			.teamEname(teamEname)
			.formation(betsLineupSide.getFormation())
			.startingLineup(toPlayers(betsLineupSide.getStartinglineup(), teamId))
			.substitutes(toPlayers(betsLineupSide.getSubstitutes(), teamId))
			.build();
	}

	public List<LineupPlayer> toPlayers(List<BetsLineupResponse.LineupPlayer> betsPlayers, String teamId) {
		if (betsPlayers == null)
			return new ArrayList<>();

		List<LineupPlayer> list = new ArrayList<>(betsPlayers.size());
		for (BetsLineupResponse.LineupPlayer betsPlayer : betsPlayers) {
			list.add(toPlayer(betsPlayer, teamId));
		}
		return list;
	}

	public LineupPlayer toPlayer(BetsLineupResponse.LineupPlayer betsPlayer, String teamId) {
		String playerApiId = null;
		String eName = null;

		if (betsPlayer != null && betsPlayer.getPlayer() != null) {
			playerApiId = betsPlayer.getPlayer().getId();
			eName = betsPlayer.getPlayer().getName();
		}

		String playerId = (teamId == null || playerApiId == null) ? null : teamId + ":" + playerApiId;

		return LineupPlayer.builder()
			.playerId(playerId)
			.eName(eName)
			.shirtNumber(parseIntOrNull(betsPlayer != null ? betsPlayer.getShirtnumber() : null))
			.position(mapPosition(betsPlayer != null ? betsPlayer.getPos() : null))
			.goals(0)
			.build();
	}

	private Integer parseIntOrNull(String v) {
		if (v == null || v.isBlank())
			return null;
		try {
			return Integer.valueOf(v);
		} catch (NumberFormatException e) {
			return null;
		}
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
