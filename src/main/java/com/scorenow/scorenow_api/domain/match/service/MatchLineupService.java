package com.scorenow.scorenow_api.domain.match.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupUpdateRequest;
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

	/* ========================= Public API - Manual ========================= */

	/** 라인업 수동 업데이트 */
	@Transactional
	public String updateMatchLineupManual(String matchId, String playerId, MatchLineupUpdateRequest request) {
		if (matchId == null || matchId.isBlank())
			throw new IllegalArgumentException("잘못된 요청: matchId가 비어있습니다.");
		if (playerId == null || playerId.isBlank())
			throw new IllegalArgumentException("잘못된 요청: playerId가 비어있습니다.");
		if (request == null)
			throw new IllegalArgumentException("잘못된 요청: request가 null입니다.");

		MatchLineupDocument doc = matchLineupRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("라인업이 존재하지 않습니다. matchId=" + matchId));

		boolean found = false;
		boolean changed = false;

		if (doc.getHome() != null) {
			UpdateResult r = updatePlayerOnSide(doc.getHome(), playerId, request);
			found |= r.found;
			changed |= r.changed;
		}
		if (doc.getAway() != null) {
			UpdateResult r = updatePlayerOnSide(doc.getAway(), playerId, request);
			found |= r.found;
			changed |= r.changed;
		}

		if (!found) {
			throw new IllegalArgumentException(
				"라인업에서 선수를 찾지 못했습니다. matchId=" + matchId + ", playerId=" + playerId);
		}

		// 변경 없으면 저장 안 하고 정상 응답
		if (!changed) {
			return "NO_CHANGES";
		}

		matchLineupRepository.save(doc);
		return matchId + ":" + playerId;
	}

	/* ========================= Manual Internals =========================== */

	private static class UpdateResult {
		final boolean found;
		final boolean changed;

		UpdateResult(boolean found, boolean changed) {
			this.found = found;
			this.changed = changed;
		}

		static UpdateResult notFound() {
			return new UpdateResult(false, false);
		}

		static UpdateResult foundNoChange() {
			return new UpdateResult(true, false);
		}

		static UpdateResult foundChanged() {
			return new UpdateResult(true, true);
		}
	}

	/** 라인업 수동 업데이트 */
	private UpdateResult updatePlayerOnSide(LineupSide side, String playerId, MatchLineupUpdateRequest req) {
		if (side == null)
			return UpdateResult.notFound();

		LineupPlayer p = findPlayer(side, playerId);
		if (p == null)
			return UpdateResult.notFound();

		boolean changed = false;

		if (req.getPosition() != null && !req.getPosition().isBlank()) {
			String newPos = req.getPosition().trim();
			if (p.getPosition() == null || !p.getPosition().equals(newPos)) {
				p.setPosition(newPos);
				changed = true;
			}
		}

		if (req.getShirtNumber() != null) {
			Integer newNo = req.getShirtNumber();
			if (p.getShirtNumber() == null || !p.getShirtNumber().equals(newNo)) {
				p.setShirtNumber(newNo);
				changed = true;
			}
		}

		if (req.getGoals() != null) {
			int newGoals = Math.max(0, req.getGoals());
			Integer cur = (p.getGoals() == null ? 0 : p.getGoals());
			if (!cur.equals(newGoals)) {
				p.setGoals(newGoals);
				changed = true;
			}
		}

		return changed ? UpdateResult.foundChanged() : UpdateResult.foundNoChange();
	}

	private LineupPlayer findPlayer(LineupSide side, String playerId) {
		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				if (playerId.equals(p.getPlayerId()))
					return p;
			}
		}
		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				if (playerId.equals(p.getPlayerId()))
					return p;
			}
		}
		return null;
	}


	/* ========================= Public API - Sync(Lineup) =================== */

	/**
	 * eventId 기준으로 라인업 조회 + Mongo 저장 (upsert)
	 * matchId 예: BETS1 + eventId
	 */
	@Transactional
	public MatchLineupDocument saveInplayMatchLineup(String matchId, String sportId) {
		if (matchId == null || matchId.isBlank())
			throw new IllegalArgumentException("잘못된 요청: matchId가 비어있습니다.");

		if (sportId == null || sportId.isBlank())
			throw new IllegalArgumentException("잘못된 요청: sportId가 비어있습니다.");

		if (!matchId.startsWith(sportId))
			throw new IllegalArgumentException(
				"잘못된 요청: matchId/sportId가 일치하지 않습니다. matchId=" + matchId + ", sportId=" + sportId);

		String eventId = MatchIdParser.extractEventId(matchId, sportId);
		BetsLineupResponse response = betsApiClient.getLineup(eventId);
		validateLineupResponse(response, eventId);

		BetsLineupResponse.BetsLineupResult results = response.getResults();

		MatchLineupDocument doc = matchLineupRepository.findById(matchId)
			.orElseGet(() -> MatchLineupDocument.create(matchId));

		doc.setHome(mapSide(results.getHome(), sportId));
		doc.setAway(mapSide(results.getAway(), sportId));

		return matchLineupRepository.save(doc);
	}

	/* ========================= Public API - Read =========================== */

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

	/* ========================= Public API - Sync(Goals) ==================== */

	/** 골득점 업데이트 */
	@Transactional
	public void updateGoals(String matchId, String sportId) {
		if (matchId == null || matchId.isBlank())
			return;
		if (sportId == null || sportId.isBlank())
			return;

		try {
			String eventId = MatchIdParser.extractEventId(matchId, sportId);
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
			int newGoalEvents = 0;
			int applied = 0;
			List<String> appliedEventIds = new ArrayList<>();

			for (var ev : result.getEvents()) {
				if (ev == null || ev.getId() == null || ev.getText() == null)
					continue;

				String text = ev.getText();
				if (!text.contains("Goal"))
					continue;

				if (redisSvc.isGoalEventSeen(matchId, ev.getId()))
					continue;

				newGoalEvents++;
				log.debug("⚽[GOALS] 신규 골 이벤트 감지 matchId={}, eventId={}, text={}",
					matchId, ev.getId(), text);

				GoalInfo goal = GoalInfo.parse(text);
				if (goal == null || goal.playerName == null || goal.playerName.isBlank())
					continue;

				if (applyGoal(doc, goal.playerName)) {
					changed = true;
					applied++;
					appliedEventIds.add(ev.getId());
					log.debug("✅[GOALS] 골 반영 matchId={}, eventId={}, player={}",
						matchId, ev.getId(), goal.playerName);
				}
			}

			if (changed) {
				matchLineupRepository.save(doc);
				for (String goalEventId : appliedEventIds) {
					redisSvc.markGoalEventSeen(matchId, goalEventId, 24 * 60 * 60);
				}
			}

			if (newGoalEvents > 0) {
				log.debug("[GOALS] 요약 matchId={}, 신규이벤트={}, 반영={}, 저장={}",
					matchId, newGoalEvents, applied, changed);
			}

		} catch (Exception e) {
			log.warn("⚠[GOALS] 처리 중 예외 matchId={}, sportId={}, reason={}",
				matchId, sportId, e.toString());
		}
	}

	/* ========================= Internals - Validation ====================== */
	private void validateLineupResponse(BetsLineupResponse response, String eventId) {
		if (response == null) {
			throw new IllegalStateException("라인업 API 응답이 null입니다. eventId=" + eventId);
		}
		if (response.getSuccess() == null || response.getSuccess() != 1) {
			throw new IllegalStateException("라인업 API 호출 실패: success=" + response.getSuccess() + ", eventId=" + eventId);
		}
		if (response.getResults() == null
			|| response.getResults().getHome() == null
			|| response.getResults().getAway() == null) {
			throw new IllegalStateException("라인업 API 결과가 비어있습니다. eventId=" + eventId);
		}
	}

	/* ========================= Internals - Mapping ========================= */
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

	/* ========================= Internals - Goal Helpers ==================== */
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
}
