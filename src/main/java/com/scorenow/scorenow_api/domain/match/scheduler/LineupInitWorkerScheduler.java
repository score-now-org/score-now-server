package com.scorenow.scorenow_api.domain.match.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.redis.InplayRedisKeys;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LineupInitWorkerScheduler {

	private final InplayRedisService redisSvc;
	private final MatchLineupService lineupSvc;

	@Value("${scorenow.inplay.worker.lineup.initBatchSize:10}")
	private int initBatchSize;

	@Value("${scorenow.inplay.due.goals.intervalMs:15000}")
	private long goalsIntervalMs;

	// 재큐잉 최대 횟수
	@Value("${scorenow.inplay.worker.lineup.maxRetry:5}")
	private int maxRetry;

	@Scheduled(fixedDelayString = "${scorenow.inplay.worker.lineup.fixedDelayMs:10000}")
	public void run() {

		for (int i = 0; i < initBatchSize; i++) {

			String payload = redisSvc.popNewLineup();
			if (payload == null)
				break;

			String[] parts = payload.split("\\|", 3);
			if (parts.length < 2) {
				log.warn("[LINEUP INIT SKIP] invalid payload={}", payload);
				continue;
			}

			String matchId = parts[0];
			String sportId = parts[1];
			int attempt = 0;

			if (parts.length == 3) {
				try {
					attempt = Integer.parseInt(parts[2]);
				} catch (NumberFormatException ignore) {
					attempt = 0;
				}
			}

			// 도큐먼트 존재 여부 확인(안전장치)
			if (lineupSvc.exists(matchId)) {
				log.debug("[LINEUP INIT SKIP] 라인업 도큐먼트가 이미 존재={}", matchId);
				continue;
			}

			// 중복 처리 방지(짧은 락)
			String lockKey = InplayRedisKeys.LOCK_LINEUP_PREFIX + matchId;

			// 락 실패 → 유실 방지를 위해 재큐잉
			if (!redisSvc.tryLock(lockKey, 10_000)) {
				redisSvc.requeueNewLineup(payload);
				log.debug("[LINEUP INIT REQUEUE] lock busy matchId={}", matchId);
				continue;
			}

			try {
				lineupSvc.fetchAndSaveByMatchId(matchId, sportId);

				long nextRunAt = System.currentTimeMillis() + goalsIntervalMs;
				redisSvc.scheduleNext(InplayRedisKeys.DUE_GOALS, matchId, nextRunAt);

				log.info("[LINEUP INIT OK] matchId={}", matchId);

			} catch (Exception e) {

				int nextAttempt = attempt + 1;

				if (nextAttempt > maxRetry) {
					redisSvc.pushLineupInitDlq(payload, e.toString());
					log.error("[LINEUP INIT DLQ] matchId={}, attempt={}, reason={}",
						matchId, nextAttempt, e.toString());
					continue;
				}

				// 예외도 유실 방지를 위해 재큐잉
				// 재시도 payload로 attempt 업데이트해서 재큐잉
				String retryPayload = matchId + "|" + sportId + "|" + nextAttempt;
				redisSvc.requeueNewLineup(retryPayload);

				log.warn("[LINEUP INIT RETRY] matchId={}, attempt={}, reason={}",
					matchId, nextAttempt, e.toString());
			}
		}
	}
}
