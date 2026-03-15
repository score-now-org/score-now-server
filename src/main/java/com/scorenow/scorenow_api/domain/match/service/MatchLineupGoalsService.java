package com.scorenow.scorenow_api.domain.match.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.util.MatchIdParser;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchLineupGoalsService {

	private final BetsApiClient betsApiClient;
	private final MatchLineupRepository matchLineupRepo;

	/**
	 * 경기 view 이벤트를 조회해서 "Goal" 이벤트를 라인업 문서 goals에 반영
	 * - Redis로 goalEventId 중복 반영 방지
	 * - 변경이 있을 때만 Mongo 저장
	 */
	@Transactional
	public void updateLineupGoals(String matchId, String sportId) {
		if (matchId == null || matchId.isBlank())
			return;
		if (sportId == null || sportId.isBlank())
			return;

		try {
			String eventId = MatchIdParser.extractEventId(matchId, sportId);

			log.info("📡[GOALS VIEW CALL] matchId={}, sportId={}, eventId={}", matchId, sportId, eventId);
			var viewResponse = betsApiClient.getEventView(eventId);
			log.info("📩[GOALS VIEW RESP] matchId={}, hasResults={}", matchId,
				viewResponse != null && viewResponse.getResults() != null && !viewResponse.getResults().isEmpty());

			if (viewResponse == null || viewResponse.getResults() == null || viewResponse.getResults().isEmpty()) {
				log.debug("⏭[GOALS] viewResponse empty matchId={}, eventId={}", matchId, eventId);
				return;
			}

			var result = viewResponse.getResults().get(0);
			List<BetsViewResponse.EventText> events = result.getEvents();
			if (events == null || events.isEmpty()) {
				log.debug("⏭[GOALS] events empty matchId={}, eventId={}", matchId, eventId);
				return;
			}

			MatchLineupDocument doc = matchLineupRepo.findById(matchId).orElse(null);
			if (doc == null) {
				log.debug("⏭[GOALS] lineup doc not found matchId={}", matchId);
				return;
			}

			// ✅ 스케줄링시 Goal 이벤트를 읽어서 선수별 득점 수 재계산
			Map<String, Integer> goalCounts = buildGoalCounts(events);

			boolean changed = false;
			changed |= applyGoalCountsToSide(doc.getHome(), goalCounts);
			changed |= applyGoalCountsToSide(doc.getAway(), goalCounts);

			if (changed) {
				matchLineupRepo.save(doc);
				log.debug("✅[GOALS] 반영 완료 matchId={}, goalPlayers={}", matchId, goalCounts.size());
			} else {
				log.debug("⏭[GOALS] 변경 없음 matchId={}", matchId);
			}

		} catch (Exception e) {
			log.warn("⚠[GOALS] 처리 중 예외 matchId={}, sportId={}, reason={}",
				matchId, sportId, e.toString());

		}
	}

	private boolean applyGoalCountsToSide(LineupSide side, Map<String, Integer> goalCounts) {
		if (side == null)
			return false;

		boolean changed = false;

		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				changed |= applyGoalCountToPlayer(p, goalCounts);
			}
		}
		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				changed |= applyGoalCountToPlayer(p, goalCounts);
			}
		}

		return changed;
	}

	private boolean applyGoalCountToPlayer(LineupPlayer p, Map<String, Integer> goalCounts) {
		if (p == null)
			return false;

		String nameKey = normalize(p.getEName());
		int newGoals = goalCounts.getOrDefault(nameKey, 0);

		Integer cur = (p.getGoals() == null ? 0 : p.getGoals());
		if (!cur.equals(newGoals)) {
			p.setGoals(newGoals);   // ✅ “+1”이 아니라 “집계값으로 세팅”
			return true;
		}
		return false;
	}

	private String normalize(String s) {
		if (s == null)
			return "";
		return s.trim().toUpperCase().replaceAll("\\s+", " ");
	}

	/**
	 * BetsViewResponse의 events에서 Goal 이벤트만 골라
	 * 선수명 기준으로 득점 수를 집계한다.
	 */
	private Map<String, Integer> buildGoalCounts(List<BetsViewResponse.EventText> events) {
		Map<String, Integer> counts = new HashMap<>();

		for (var ev : events) {
			if (ev == null || ev.getText() == null)
				continue;

			String text = ev.getText();
			if (!text.contains("Goal"))
				continue;

			GoalInfo goal = GoalInfo.parse(text);
			if (goal == null || goal.playerName == null || goal.playerName.isBlank())
				continue;

			String key = normalize(goal.playerName);
			counts.put(key, counts.getOrDefault(key, 0) + 1);
		}

		return counts;

	}

	/** */
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

}
