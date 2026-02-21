package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisKeys;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoalsDueScheduler {

	private final InplayRedisService redisSvc;
	private final MatchLineupService lineupSvc;
	private final MatchRepository matchRepo;

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
		int removedNotInplay = 0;
		int failed = 0;

		for (String payload : payloads) {

			if (payload == null || payload.isBlank())
				continue;

			String[] parts = payload.split("\\|", 2);
			String matchId = parts[0];
			String sportId = parts[1];

			String lockKey = InplayRedisKeys.LOCK_GOALS_PREFIX + matchId;

			// 락 획득 실패 시 스킵
			if (!redisSvc.tryLock(lockKey, lockTtlMs)) {
				skippedLock++;
				continue;
			}

			try {
				// IN_PLAY 아니면 due 제거
				if (!matchRepo.existsByIdAndStatusCode(matchId, MatchStatus.IN_PLAY)) {
					redisSvc.removeDue(InplayRedisKeys.DUE_GOALS, payload);
					removedNotInplay++;
					continue;
				}

				// 골 업데이트
				lineupSvc.updateGoals(matchId, sportId);

				// payload 재예약
				redisSvc.scheduleNext(InplayRedisKeys.DUE_GOALS, payload, now + intervalMs);

				handled++;

			} catch (Exception e) {
				failed++;
				log.warn("⚠[GOALS DUE] 실패: 골 업데이트/재예약 중 예외 matchId={}, reason={}",
					matchId, e.toString());

				// 예외 시 재예약(너 로직 유지)
				redisSvc.scheduleNext(
					InplayRedisKeys.DUE_GOALS,
					payload,
					now + Math.min(30_000L, intervalMs)
				);
			}
		}

		log.debug("[GOALS DUE] 처리={}, 스킵(락)={}, 제거(IN_PLAY아님)={}, 실패={}",
			handled, skippedLock, removedNotInplay, failed);
	}
}
