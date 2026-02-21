package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.service.InplayScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InplayScanScheduler {

	private final InplayScanService scanSvc;
	private final InplayRedisService redisSvc;
	private final MatchLineupRepository lineupRepo;

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

		/** doc 없는 경기만 enqueue */
		for (InplayScanDto dto : unique) {

			if (existingIds.contains(dto.getMatchId())) {
				docSkipped++;
				continue;
			}

			boolean first = redisSvc.markSeenAndEnqueue(dto.getMatchId(), dto.getSportId());
			if (first)
				newlyQueued++;
			else
				alreadySeen++;
		}

		log.info("[INPLAY SCAN] 감지 경기={}, 신규 큐잉={}, 스킵(doc존재)={}, 스킵(이미큐잉)={}",
			inplayMatchIds.size(), newlyQueued, docSkipped, alreadySeen);
	}
}