package com.scorenow.scorenow_api.domain.match.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.dto.InplayMatchDetectionDto;
import com.scorenow.scorenow_api.domain.match.service.InplayMatchDetectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InplayMatchDetectionScheduler {

	private final InplayMatchDetectionService inplayMatchSvc;

	/**
	 * IN_PLAY 경기 식별 스케줄러
	 * DB에서 IN_PLAY 경기 목록을 한 묶음으로 조회
	 */
	@Scheduled(fixedDelay = 5000) // 테스트 5초(운영 시 60초)
	public void run() {
		List<InplayMatchDetectionDto> newOnes = inplayMatchSvc.getNewInplayMatchesAndMark();
		log.info("[IN_PLAY NEW] count={}", newOnes.size());

		newOnes.stream().limit(3).forEach(m ->
			log.info("[IN_PLAY NEW] matchId={}, HOME={}({}) AWAY={}({})",
				m.getMatchId(),
				m.getHomeId(), m.getHomeName(),
				m.getAwayId(), m.getAwayName()
			)
		);
	}
}
