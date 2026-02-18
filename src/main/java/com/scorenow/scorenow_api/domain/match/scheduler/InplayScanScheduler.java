package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.service.InplayScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InplayScanScheduler {

	private final InplayScanService scanSvc;
	private final InplayRedisService redisSvc;

	@Scheduled(fixedDelayString = "${scorenow.inplay.scan.fixedDelayMs:60000}")
	public void run() {
		
		List<InplayScanDto> inplay = scanSvc.scanInplayMatches();

		if (inplay.isEmpty()) {
			log.debug("[INPLAY SCAN] inplay=0");
			return;
		}

		int newlyQueued = 0;

		for (InplayScanDto dto : inplay) {
			boolean first = redisSvc.markSeenAndEnqueue(
				dto.getMatchId(),
				dto.getSportId()
			);
			if (first)
				newlyQueued++;
		}

		log.info("[INPLAY SCAN] inplay={}, newlyQueued={}", inplay.size(), newlyQueued);
	}

}
