package com.scorenow.scorenow_api.domain.match.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

		Boolean ok = redis.opsForValue().setIfAbsent(seenKey, "1", Duration.ofSeconds(seenTtlSeconds));
		if (ok == null || !ok) {
			return false; // 이미 본 경기
		}

		// 처음 본 경기만 큐에 넣는다
		String payload = matchId + "|" + sportId;
		redis.opsForList().leftPush(InplayRedisKeys.Q_NEW_LINEUP, payload);
		redis.opsForList().leftPush(InplayRedisKeys.Q_NEW_DETAIL, payload);
		return true;
	}

	/** 큐에서 1개 pop (없으면 null) */
	public String popNewLineup() {
		return redis.opsForList().rightPop(InplayRedisKeys.Q_NEW_LINEUP);
	}

	public String popNewDetail() {
		return redis.opsForList().rightPop(InplayRedisKeys.Q_NEW_DETAIL);
	}

	/** due ZSET에서 nowMillis 이하인 것 N개 뽑기 */
	public List<String> pollDue(String dueKey, long nowMillis, int limit) {
		Set<String> set = redis.opsForZSet().rangeByScore(dueKey, 0, nowMillis, 0, limit);
		if (set == null || set.isEmpty())
			return List.of();
		return new ArrayList<>(set);
	}

	public void requeueNewLineup(String payload) {
		redis.opsForList().rightPush(InplayRedisKeys.Q_NEW_LINEUP, payload);
	}

	public void pushLineupInitDlq(String payload, String reason) {
		String r = (reason == null) ? "" : reason;
		if (r.length() > 300)
			r = r.substring(0, 300);

		String dlqPayload = payload + "|reason=" + r;
		redis.opsForList().rightPush(InplayRedisKeys.DLQ_LINEUP_INIT, dlqPayload);
	}

	/** due 재등록(다음 실행 시각) */
	public void scheduleNext(String dueKey, String matchId, long nextRunAtMillis) {
		redis.opsForZSet().add(dueKey, matchId, nextRunAtMillis);
	}

	/** due에서 제거 */
	public void removeDue(String dueKey, String matchId) {
		redis.opsForZSet().remove(dueKey, matchId);
	}

	/** 짧은 락 (중복 처리 방지). 획득 성공이면 true */
	public boolean tryLock(String lockKey, long ttlMillis) {
		Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMillis(ttlMillis));
		return ok != null && ok;
	}

	public boolean markGoalEventIfNew(String matchId, String goalEventId, long ttlSeconds) {
		String key = InplayRedisKeys.GOAL_SEEN_PREFIX + matchId + ":" + goalEventId;
		Boolean ok = redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
		return ok != null && ok;
	}

	public boolean isGoalEventSeen(String matchId, String goalEventId) {
		String key = InplayRedisKeys.GOAL_SEEN_PREFIX + matchId + ":" + goalEventId;
		Boolean exists = redis.hasKey(key);
		return Boolean.TRUE.equals(exists);
	}

	public void markGoalEventSeen(String matchId, String goalEventId, long ttlSeconds) {
		String key = InplayRedisKeys.GOAL_SEEN_PREFIX + matchId + ":" + goalEventId;
		redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
	}

}
