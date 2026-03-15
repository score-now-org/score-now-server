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
		BetsLineupResponse.BetsLineupSide ext,
		String teamId,
		String teamEname
	) {
		if (ext == null) {
			// ext가 null이면 최소 구조라도 만들어두는 게 안전함
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
			.formation(ext.getFormation())
			.startingLineup(toPlayers(ext.getStartinglineup(), teamId))
			.substitutes(toPlayers(ext.getSubstitutes(), teamId))
			.build();
	}

	public List<LineupPlayer> toPlayers(List<BetsLineupResponse.BetsLineupPlayer> extPlayers, String teamId) {
		if (extPlayers == null)
			return new ArrayList<>();

		List<LineupPlayer> list = new ArrayList<>(extPlayers.size());
		for (BetsLineupResponse.BetsLineupPlayer ext : extPlayers) {
			list.add(toPlayer(ext, teamId));
		}
		return list;
	}

	public LineupPlayer toPlayer(BetsLineupResponse.BetsLineupPlayer ext, String teamId) {
		String playerApiId = null;
		String eName = null;

		if (ext != null && ext.getPlayer() != null) {
			playerApiId = ext.getPlayer().getId();
			eName = ext.getPlayer().getName();
		}

		String playerId = (teamId == null || playerApiId == null) ? null : teamId + ":" + playerApiId;

		return LineupPlayer.builder()
			.playerId(playerId)
			.eName(eName)
			.shirtNumber(parseIntOrNull(ext != null ? ext.getShirtnumber() : null))
			.position(mapPosition(ext != null ? ext.getPos() : null))
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
