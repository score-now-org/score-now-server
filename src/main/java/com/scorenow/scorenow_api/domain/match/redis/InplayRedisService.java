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

	// ======================================================================
	// 1) 신규 경기 큐잉 (Scan -> Queue)
	// ======================================================================

	/**
	 * IN_PLAY 스캔 결과를 받아 "처음 본 경기"만 큐에 enqueue
	 * - seenKey: inplay:seen:{matchId} (SETNX + TTL)
	 * - 신규면 lineup/detail 큐에 payload(matchId|sportId) push
	 */
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

	/** 라인업 신규 큐 재큐잉 */
	public void requeueNewLineup(String payload) {
		redis.opsForList().rightPush(InplayRedisKeys.Q_NEW_LINEUP, payload);
	}

	// ======================================================================
	// 2) 큐 소비 (Worker)
	// ======================================================================

	/** 라인업 신규 큐에서 1개 pop (없으면 null) */
	public String popNewLineup() {
		return redis.opsForList().rightPop(InplayRedisKeys.Q_NEW_LINEUP);
	}

	/** 디테일 신규 큐에서 1개 pop (없으면 null) */
	public String popNewDetail() {
		return redis.opsForList().rightPop(InplayRedisKeys.Q_NEW_DETAIL);
	}

	// ======================================================================
	// 3) 예약 실행 (Due ZSET)
	// ======================================================================

	/**
	 * due ZSET에서 nowMillis 이하(score <= nowMillis)인 payload를 최대 limit개 뽑는다.
	 * - payload: matchId|sportId
	 * - score는 "다음 실행 시각(ms)"로 사용
	 */
	public List<String> fetchDue(String dueKey, long nowMillis, int limit) {
		Set<String> set = redis.opsForZSet().rangeByScore(dueKey, 0, nowMillis, 0, limit);
		if (set == null || set.isEmpty())
			return List.of();
		return new ArrayList<>(set);
	}

	/** due 재등록(다음 실행 시각) */
	public void scheduleNext(String dueKey, String payload, long nextRunAtMillis) {
		redis.opsForZSet().add(dueKey, payload, nextRunAtMillis);
	}

	/** due에서 제거 */
	public void removeDue(String dueKey, String payload) {
		redis.opsForZSet().remove(dueKey, payload);
	}

	// ======================================================================
	// 4) 동시성 제어 (Lock)
	// ======================================================================

	/** 짧은 락 (중복 처리 방지). 획득 성공이면 true */
	public boolean tryLock(String lockKey, long ttlMillis) {
		Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMillis(ttlMillis));
		return ok != null && ok;
	}

	// ======================================================================
	// 5) 이벤트 중복 방지 (Goal dedup)
	// ======================================================================

	/** goalEventId가 이미 처리되었는지 확인 */
	public boolean isGoalEventSeen(String matchId, String goalEventId) {
		String key = InplayRedisKeys.GOAL_SEEN_PREFIX + matchId + ":" + goalEventId;
		Boolean exists = redis.hasKey(key);
		return Boolean.TRUE.equals(exists);
	}

	/** goalEventId를 처리 완료로 마킹(SET + TTL) */
	public void markGoalEventSeen(String matchId, String goalEventId, long ttlSeconds) {
		String key = InplayRedisKeys.GOAL_SEEN_PREFIX + matchId + ":" + goalEventId;
		redis.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
	}

	// ======================================================================
	// 6) 장애 보관 (DLQ)
	// ======================================================================

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
