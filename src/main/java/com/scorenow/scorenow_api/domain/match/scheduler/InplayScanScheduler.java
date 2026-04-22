package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisKeys;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.service.InplayScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class InplayScanScheduler {

	private final InplayScanService scanSvc;
	private final InplayRedisService redisSvc;
	private final MatchLineupRepository lineupRepo;

	@Value("${scorenow.inplay.due.goals.intervalMs:15000}")
	private long goalsIntervalMs;

	@Scheduled(fixedDelayString = "${scorenow.inplay.scan.fixedDelayMs:60000}")
	public void run() {

		List<InplayScanDto> inplay = scanSvc.scanInplayMatches();
		if (inplay.isEmpty()) {
			log.debug("[INPLAY SCAN] 감지 경기 없음");
			return;
		}

		/** IN_PLAY 경기 matchId 리스트 추출 */
		List<InplayScanDto> unique = inplay.stream()
			.collect(Collectors.toMap(
				InplayScanDto::getMatchId,
				dto -> dto,
				(a, b) -> a
			))
			.values()
			.stream()
			.toList();

		List<String> inplayMatchIds = unique.stream()
			.map(InplayScanDto::getMatchId)
			.toList();

		Set<String> existingIds = lineupRepo.findByIdIn(inplayMatchIds).stream()
			.map(MatchLineupDocument::getId)
			.collect(Collectors.toSet());

		int docSkipped = 0;
		int alreadySeen = 0;
		int newlyQueued = 0;

		long now = System.currentTimeMillis();

		for (InplayScanDto dto : unique) {

			String matchId = dto.getMatchId();
			String sportId = dto.getSportId();
			String basePayload = matchId + "|" + sportId;

			// ✅[ADD] 이번 스캔에서 IN_PLAY로 본 경기 -> alive TTL 갱신(heartbeat)
			redisSvc.touchInplayAlive(matchId);

			// ✅ GOALS는 IN_PLAY 감지 시점부터 주기 호출 대상 등록 (없을 때만)
			redisSvc.scheduleNextIfAbsent(
				InplayRedisKeys.DUE_GOALS,
				basePayload,
				now + goalsIntervalMs
			);

			// ✅ 라인업 생성은 doc 없는 경기만 NEW_LINEUP에 enqueue
			if (existingIds.contains(matchId)) {
				docSkipped++;
				continue;
			}

			boolean first = redisSvc.markSeenAndEnqueue(matchId, sportId);
			if (first)
				newlyQueued++;
			else
				alreadySeen++;
		}

		log.info("[INPLAY SCAN] 감지 경기={}, 신규 큐잉={}, 스킵(doc존재)={}, 스킵(이미큐잉)={}",
			inplayMatchIds.size(), newlyQueued, docSkipped, alreadySeen);
	}
}