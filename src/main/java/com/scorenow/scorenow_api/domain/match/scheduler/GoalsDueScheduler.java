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
import com.scorenow.scorenow_api.domain.match.util.MatchIdParser;

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
		log.info("[GoalsDueScheduler] 골득점 스케줄러 시작");

		long now = System.currentTimeMillis();

		// due된 matchId 조회
		List<String> matchIds = redisSvc.pollDue(InplayRedisKeys.DUE_GOALS, now, batchSize);
		log.info("[GOALS DUE POLL] size={}, ids={}", matchIds.size(), matchIds);

		if (matchIds.isEmpty())
			return;

		for (String matchId : matchIds) {
			log.info("[GOALS DUE HANDLE] matchId={}", matchId);
			if (matchId == null || matchId.isBlank())
				continue;

			String lockKey = InplayRedisKeys.LOCK_GOALS_PREFIX + matchId;

			// 락 획득 실패 시 스킵
			if (!redisSvc.tryLock(lockKey, lockTtlMs))
				continue;

			try {

				if (!matchRepo.existsByIdAndStatusCode(matchId, MatchStatus.IN_PLAY)) {
					log.info("[GOALS DUE STOP] not IN_PLAY matchId={}", matchId);
					redisSvc.removeDue(InplayRedisKeys.DUE_GOALS, matchId);
					continue;
				}

				String sportId = MatchIdParser.extractSportId(matchId);

				// 골 업데이트 (view api 호출 + 반영)
				lineupSvc.updateGoals(matchId, sportId);

				// 다음 실행 예약
				redisSvc.scheduleNext(InplayRedisKeys.DUE_GOALS, matchId, now + intervalMs);

			} catch (Exception e) {
				log.warn("GoalsDueScheduler error. matchId={}", matchId, e);

				// 예외 시에는 다음 시도 예약만 하고 due는 남아있음
				redisSvc.scheduleNext(
					InplayRedisKeys.DUE_GOALS,
					matchId,
					now + Math.min(30_000L, intervalMs)
				);
			}
		}
	}
}
