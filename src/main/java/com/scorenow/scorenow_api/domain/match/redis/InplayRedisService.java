package com.scorenow.scorenow_api.domain.match.redis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InplayRedisService {

	private final StringRedisTemplate redis;

	@Value("${scorenow.inplay.seen.ttlSeconds:21600}")
	private long seenTtlSeconds;

	public boolean markSeenAndEnqueue(String matchId, String sportId) {
		String seenKey = InplayRedisKeys.SEEN_PREFIX + matchId;

		Boolean ok = redis.opsForValue().setIfAbsent(
			seenKey, "1", Duration.ofSeconds(seenTtlSeconds)
		);

		if (ok == null || !ok) {
			return false;
		}

		String payload = matchId + "|" + sportId;
		redis.opsForList().leftPush(InplayRedisKeys.Q_NEW_LINEUP, payload);
		return true;
	}

	/** 라인업 신규 큐 재큐잉 */
	public void requeueNewLineup(String payload) {
		redis.opsForList().rightPush(InplayRedisKeys.Q_NEW_LINEUP, payload);
	}

	/** 라인업 신규 큐에서 1개 pop (없으면 null) */
	public String popNewLineup() {
		return redis.opsForList().rightPop(InplayRedisKeys.Q_NEW_LINEUP);
	}

	/** 짧은 락 (중복 처리 방지). 획득 성공이면 true */
	public boolean tryLock(String lockKey, long ttlMillis) {
		Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMillis(ttlMillis));
		return ok != null && ok;
	}

	/**
	 * 라인업 초기화 실패 시 DLQ로 이동
	 * reason은 최대 300자
	 */
	public void pushLineupInitDlq(String payload, String reason) {
		String r = (reason == null) ? "" : reason;
		if (r.length() > 300)
			r = r.substring(0, 300);

		String dlqPayload = payload + "|reason=" + r;
		redis.opsForList().rightPush(InplayRedisKeys.DLQ_LINEUP_INIT, dlqPayload);
	}
}
