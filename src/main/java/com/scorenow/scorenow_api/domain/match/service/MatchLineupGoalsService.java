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
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsFootballViewResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsFootballViewResponse.ViewResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchLineupGoalsService {

	private final BetsApiClient betsApiClient;
	private final MatchLineupRepository matchLineupRepo;

	@Transactional
	public void updateLineupGoals(MatchLineupDocument matchLineupDocument, ViewResult matchDetail) {
		List<BetsFootballViewResponse.EventText> events = matchDetail.getEvents();

		if (events == null || events.isEmpty()) {
			log.info("⏭[GOALS] events empty apiMatchId={}", matchDetail.getId());
			return;
		}

		Map<String, Integer> goalCounts = buildGoalCounts(events);

		boolean changed = false;
		changed |= applyGoalCountsToSide(matchLineupDocument.getHome(), goalCounts);
		changed |= applyGoalCountsToSide(matchLineupDocument.getAway(), goalCounts);

		if (changed) {
			matchLineupRepo.save(matchLineupDocument);
			log.info("✅[GOALS] 반영 완료 matchId={}, goalPlayers={}", matchLineupDocument.getId(), goalCounts.size());
		} else {
			log.info("⏭[GOALS] 변경 없음 matchId={}", matchLineupDocument.getId());
		}
	}

	private boolean applyGoalCountsToSide(LineupSide side, Map<String, Integer> goalCounts) {
		if (side == null) {
			return false;
		}

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
		if (p == null) {
			return false;
		}

		String playerName = normalize(p.getEName());

		int newGoals = 0;
		for (Map.Entry<String, Integer> entry : goalCounts.entrySet()) {
			String scorerName = entry.getKey();

			if (playerName.contains(scorerName)) {
				newGoals += entry.getValue();
			}
		}

		Integer cur = (p.getGoals() == null ? 0 : p.getGoals());
		if (!cur.equals(newGoals)) {
			p.setGoals(newGoals);
			return true;
		}
		return false;
	}

	private String normalize(String s) {
		return s == null ? "" : s.trim().toUpperCase();
	}

	private Map<String, Integer> buildGoalCounts(List<BetsFootballViewResponse.EventText> events) {
		Map<String, Integer> counts = new HashMap<>();

		for (var ev : events) {
			if (ev == null || ev.getText() == null) {
				continue;
			}

			String text = ev.getText();
			if (!text.contains("Goal")) {
				continue;
			}

			GoalInfo goal = GoalInfo.parse(text);
			if (goal == null || goal.playerName == null || goal.playerName.isBlank()) {
				continue;
			}

			String key = normalize(goal.playerName);
			counts.put(key, counts.getOrDefault(key, 0) + 1);
		}

		return counts;
	}

	private static class GoalInfo {
		final String playerName;

		GoalInfo(String playerName) {
			this.playerName = playerName;
		}

		static GoalInfo parse(String text) {
			try {
				int idx = text.indexOf("Goal -");
				if (idx < 0) {
					return null;
				}

				String after = text.substring(idx + "Goal -".length()).trim();

				int teamStart = after.indexOf("(");
				if (teamStart < 0) {
					return null;
				}

				String player = after.substring(0, teamStart).trim();
				if (player.isBlank()) {
					return null;
				}

				return new GoalInfo(player);
			} catch (Exception e) {
				return null;
			}
		}
	}
}