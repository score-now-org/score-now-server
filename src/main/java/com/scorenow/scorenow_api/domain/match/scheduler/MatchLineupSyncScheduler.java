package com.scorenow.scorenow_api.domain.match.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.redis.InplayRedisKeys;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchLineupSyncScheduler {

	private final InplayRedisService redisSvc;
	private final MatchLineupSyncService lineupSyncSvc;

	@Value("${scorenow.inplay.worker.lineup.initBatchSize:10}")
	private int initBatchSize;

	@Value("${scorenow.inplay.due.goals.intervalMs:15000}")
	private long goalsIntervalMs;

	// 재큐잉 최대 횟수
	@Value("${scorenow.inplay.worker.lineup.maxRetry:5}")
	private int maxRetry;

	@Value("${scorenow.inplay.worker.lineup.lockTtlMs:10000}")
	private long lockTtlMs;

	@Scheduled(fixedDelayString = "${scorenow.inplay.worker.lineup.fixedDelayMs:10000}")
	public void run() {

		for (int i = 0; i < initBatchSize; i++) {

			String payload = redisSvc.popNewLineup();
			if (payload == null)
				break;

			String[] parts = payload.split("\\|", 3);
			if (parts.length < 2) {
				log.warn("❌[LINEUP INIT] 스킵: payload 형식 오류 payload={}", payload);
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
			if (lineupSyncSvc.exists(matchId)) {
				log.debug("⏭[LINEUP INIT] 스킵: 라인업 도큐먼트 이미 존재 matchId={}", matchId);
				continue;
			}

			// 중복 처리 방지(짧은 락)
			String lockKey = InplayRedisKeys.LOCK_LINEUP_PREFIX + matchId;

			// 락 실패 → 유실 방지를 위해 재큐잉
			if (!redisSvc.tryLock(lockKey, lockTtlMs)) {
				redisSvc.requeueNewLineup(payload);
				log.debug("❗[LINEUP INIT] 재큐잉: 락 점유 중 matchId={}", matchId);
				continue;
			}

			String basePayload = matchId + "|" + sportId;

			try {
				lineupSyncSvc.syncMatchLineup(matchId, sportId);
				log.info("✅[LINEUP INIT] 완료 matchId={}", matchId);

			} catch (Exception e) {

				int nextAttempt = attempt + 1;

				if (nextAttempt > maxRetry) {
					redisSvc.pushLineupInitDlq(basePayload + "|" + nextAttempt, e.toString());
					log.error("⛔[LINEUP INIT] DLQ: 최대 재시도 초과 matchId={}, attempt={}, reason={}",
						matchId, nextAttempt, e.toString());
					continue;
				}

				// 재큐잉
				redisSvc.requeueNewLineup(basePayload + "|" + nextAttempt);

				log.warn("⚠[LINEUP INIT] 재시도: 실패 후 재큐잉 matchId={}, attempt={}, reason={}",
					matchId, nextAttempt, e.toString());
			}
		}
	}
}
