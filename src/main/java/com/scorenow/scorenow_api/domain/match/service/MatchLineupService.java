package com.scorenow.scorenow_api.domain.match.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.util.MatchIdParser;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchLineupService {

	private final BetsApiClient betsApiClient;
	private final MatchLineupRepository matchLineupRepository;
	private final InplayRedisService redisSvc;

	/**
	 * eventId 기준으로 라인업 조회 + Mongo 저장 (upsert)
	 * matchId 예: BETS1 + eventId
	 */
	@Transactional
	public MatchLineupDocument fetchAndSaveByMatchId(String matchId, String sportId) {
		if (matchId == null || matchId.isBlank())
			throw new IllegalArgumentException("matchId is blank");

		if (sportId == null || sportId.isBlank())
			throw new IllegalArgumentException("sportId is blank");

		if (!matchId.startsWith(sportId))
			throw new IllegalArgumentException("matchId/sportId mismatch");

		String eventId = matchId.substring(sportId.length());
		BetsLineupResponse response = betsApiClient.getLineup(eventId);
		validateLineupResponse(response, eventId);

		BetsLineupResponse.BetsLineupResult results = response.getResults();

		MatchLineupDocument doc = matchLineupRepository.findById(matchId)
			.orElseGet(() -> MatchLineupDocument.create(matchId));

		doc.setHome(mapSide(results.getHome(), sportId));
		doc.setAway(mapSide(results.getAway(), sportId));

		return matchLineupRepository.save(doc);
	}

	/** 도큐먼트 존재 여부 */
	@Transactional(readOnly = true)
	public boolean exists(String matchId) {
		if (matchId == null || matchId.isBlank())
			return false;
		return matchLineupRepository.existsById(matchId);
	}

	/** 라인업 조회 */
	@Transactional(readOnly = true)
	public MatchLineupDocument getByMatchId(String matchId) {
		return matchLineupRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("라인업이 존재하지 않습니다. matchId=" + matchId));
	}

	/** 골득접 업데이트 */
	public void updateGoals(String matchId, String sportId) {
		if (matchId == null || matchId.isBlank())
			return;
		if (!matchLineupRepository.existsById(matchId))
			return;

		String eventId = MatchIdParser.extractEventId(matchId);
		var viewResponse = betsApiClient.getEventView(eventId);

		if (viewResponse == null || viewResponse.getResults() == null || viewResponse.getResults().isEmpty())
			return;

		var result = viewResponse.getResults().get(0);
		if (result.getEvents() == null || result.getEvents().isEmpty())
			return;

		MatchLineupDocument doc = matchLineupRepository.findById(matchId).orElse(null);
		if (doc == null)
			return;

		boolean changed = false;

		for (var ev : result.getEvents()) {
			if (ev == null || ev.getId() == null || ev.getText() == null)
				continue;

			String text = ev.getText();
			if (!text.contains("Goal"))
				continue; // 골 이벤트만

			// ✅ 중복 방지: 같은 goalEventId는 1번만 반영
			boolean isNew = redisSvc.markGoalEventIfNew(matchId, ev.getId(), 24 * 60 * 60);
			if (!isNew)
				continue;

			log.info("[GOAL EVENT NEW] matchId={}, text={}", matchId, text);

			GoalInfo goal = GoalInfo.parse(text);
			if (goal == null || goal.playerName == null || goal.playerName.isBlank())
				continue;

			boolean applied = applyGoal(doc, goal.playerName);
			if (applied)
				changed = true;
		}

		if (changed) {
			matchLineupRepository.save(doc);
		}

	}

	/* ====================================================================== */
	private void validateLineupResponse(BetsLineupResponse response, String eventId) {
		if (response == null) {
			throw new IllegalStateException("라인업 API 응답이 null 입니다. eventId=" + eventId);
		}
		if (response.getSuccess() == null || response.getSuccess() != 1) {
			throw new IllegalStateException("라인업 API 실패. success=" + response.getSuccess() + ", eventId=" + eventId);
		}
		if (response.getResults() == null
			|| response.getResults().getHome() == null
			|| response.getResults().getAway() == null) {
			throw new IllegalStateException("라인업 API results가 비어있습니다. eventId=" + eventId);
		}
	}

	private LineupSide mapSide(BetsLineupResponse.BetsLineupSide ext, String sportId) {
		return LineupSide.builder()
			.formation(ext.getFormation())
			.startingLineup(mapPlayers(ext.getStartinglineup(), sportId))
			.substitutes(mapPlayers(ext.getSubstitutes(), sportId))
			.build();
	}

	private List<LineupPlayer> mapPlayers(List<BetsLineupResponse.BetsLineupPlayer> extPlayers,
		String sportId) {
		if (extPlayers == null) {
			return new ArrayList<>();
		}
		List<LineupPlayer> list = new ArrayList<>();
		for (BetsLineupResponse.BetsLineupPlayer ext : extPlayers) {
			list.add(mapPlayer(ext, sportId));
		}
		return list;
	}

	private LineupPlayer mapPlayer(BetsLineupResponse.BetsLineupPlayer ext, String sportId) {
		String playerId = null;
		String eName = null;

		if (ext != null && ext.getPlayer() != null) {
			playerId = ext.getPlayer().getId();
			eName = ext.getPlayer().getName();
		}

		return LineupPlayer.builder()
			.playerId(playerId == null ? null : sportId + playerId)
			.eName(eName)
			.shirtNumber(parseIntOrNull(ext != null ? ext.getShirtnumber() : null))
			.position(mapPosition(ext != null ? ext.getPos() : null))
			.goals(0) // view api 연동 전
			.build();
	}

	/* ====================================================================== */
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

	private static class GoalInfo {
		final String playerName;

		GoalInfo(String playerName) {
			this.playerName = playerName;
		}

		static GoalInfo parse(String text) {
			try {
				int idx = text.indexOf("Goal -");
				if (idx < 0)
					return null;

				String after = text.substring(idx + "Goal -".length()).trim();

				// "Wirtz  (Liverpool) - Shot ..."
				int teamStart = after.indexOf("(");
				if (teamStart < 0)
					return null;

				String player = after.substring(0, teamStart).trim();
				if (player.isBlank())
					return null;

				return new GoalInfo(player);
			} catch (Exception e) {
				return null;
			}
		}
	}

	private boolean applyGoal(MatchLineupDocument doc, String playerName) {
		if (doc.getHome() != null && incrementOnSide(doc.getHome(), playerName))
			return true;
		if (doc.getAway() != null && incrementOnSide(doc.getAway(), playerName))
			return true;
		return false;
	}

	private boolean incrementOnSide(LineupSide side, String playerName) {
		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				if (samePlayer(p.getEName(), playerName)) {
					p.setGoals((p.getGoals() == null ? 0 : p.getGoals()) + 1);
					return true;
				}
			}
		}
		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				if (samePlayer(p.getEName(), playerName)) {
					p.setGoals((p.getGoals() == null ? 0 : p.getGoals()) + 1);
					return true;
				}
			}
		}
		return false;
	}

	private boolean samePlayer(String eName, String playerNameFromEvent) {
		if (eName == null || playerNameFromEvent == null)
			return false;

		String a = eName.trim().toUpperCase();
		String b = playerNameFromEvent.trim().toUpperCase();

		if (a.equals(b))
			return true;

		return a.contains(b) || b.contains(a);
	}

	// private String extractEventId(String matchId, String sportId) {
	// 	if (!matchId.startsWith(sportId)) {
	// 		throw new IllegalStateException(
	// 			"matchId/sportId mismatch. matchId=" + matchId + ", sportId=" + sportId
	// 		);
	// 	}
	// 	return matchId.substring(sportId.length());
	// }

}
