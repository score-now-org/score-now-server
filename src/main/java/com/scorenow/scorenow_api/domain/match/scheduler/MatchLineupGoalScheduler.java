package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.redis.InplayRedisKeys;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupGoalsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchLineupGoalScheduler {

	private final InplayRedisService redisSvc;
	private final MatchLineupGoalsService lineupGoalsSyncSvc;

	@Value("${scorenow.inplay.due.goals.batchSize:20}")
	private int batchSize;

	@Value("${scorenow.inplay.due.goals.intervalMs:15000}")
	private long intervalMs;

	@Value("${scorenow.inplay.due.goals.lockTtlMs:10000}")
	private long lockTtlMs;

	@Scheduled(fixedDelayString = "${scorenow.inplay.due.goals.fixedDelayMs:5000}")
	public void run() {

		long now = System.currentTimeMillis();

		// ✅ payload 조회 (matchId|sportId)
		List<String> payloads =
			redisSvc.fetchDue(InplayRedisKeys.DUE_GOALS, now, batchSize);

		if (payloads.isEmpty()) {
			log.debug("⏭[GOALS DUE] 처리 대상 없음");
			return;
		}

		int handled = 0;
		int skippedLock = 0;
		int removedNotAlive = 0;
		int failed = 0;

		for (String payload : payloads) {

			if (payload == null || payload.isBlank())
				continue;

			String[] parts = payload.split("\\|", 3);
			if (parts.length < 2) {
				log.warn("❌[GOALS DUE] 스킵: payload 형식 오류 payload={}", payload);
				redisSvc.removeDue(InplayRedisKeys.DUE_GOALS, payload);
				continue;
			}
			String matchId = parts[0];
			String sportId = parts[1];
			String basePayload = matchId + "|" + sportId;

			String lockKey = InplayRedisKeys.LOCK_GOALS_PREFIX + matchId;

			// 락 획득 실패 시 스킵
			if (!redisSvc.tryLock(lockKey, lockTtlMs)) {
				skippedLock++;
				continue;
			}

			try {
				// alive가 없으면 "Scan에서 더 이상 IN_PLAY로 못 봄" => due 제거
				if (!redisSvc.isInplayAlive(matchId)) {
					redisSvc.removeDue(InplayRedisKeys.DUE_GOALS, payload);
					removedNotAlive++;
					continue;
				}
				// 골 업데이트
				lineupGoalsSyncSvc.updateLineupGoals(matchId, sportId);

				// payload 재예약
				long nextDue = System.currentTimeMillis() + intervalMs;
				redisSvc.scheduleNext(InplayRedisKeys.DUE_GOALS, basePayload, nextDue);

				handled++;

			} catch (Exception e) {
				failed++;
				log.warn("⚠[GOALS DUE] 실패: 골 업데이트/재예약 중 예외 matchId={}, reason={}",
					matchId, e.toString());

				// 예외 시 재예약
				long nextDue = System.currentTimeMillis() + Math.min(30_000L, intervalMs);
				redisSvc.scheduleNext(InplayRedisKeys.DUE_GOALS, basePayload, nextDue);
			}
		}

		log.debug("[GOALS DUE] 처리={}, 스킵(락)={}, 제거(alive없음)={}, 실패={}",
			handled, skippedLock, removedNotAlive, failed);

	}
}
