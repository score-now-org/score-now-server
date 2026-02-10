package com.scorenow.scorenow_api.domain.match.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSyncScheduler {

	// 지원 종목 ID(축구 , 확장 가능)
	private static final String FOOTBALL = "1";
	private final MatchSyncService matchSyncService;

	/**
	 * 예정 경기 동기화(Upcoming)
	 * 일주일치 데이터 가져오기
	 */
	@Scheduled(cron = "0 0 0,6,12,18 * * *")
	public void syncUpcomingMatches(){
		log.info("=== [UPCOMING] 예정 경기 동기화 ===");

		int totalCount = 0;
		LocalDate today = LocalDate.now();

		try{
			// 오늘 경기
			int todayCount = matchSyncService.syncUpcomingMatches(FOOTBALL, null);

			// 내일 경기
			String tomorrow = java.time.LocalDate.now().plusDays(1)
				.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
			int tomorrowCount = matchSyncService.syncUpcomingMatches(FOOTBALL, tomorrow);

			log.info("[UPCOMING] 동기화 완료 - 오늘 {}건, 내일: {}건", todayCount, tomorrowCount);
		} catch (Exception e) {
			log.error("[UPCOMING] 동기화 실패", e);
		}
	}

	/**
	 * 종료 경기 동기화(Ended)
	 * - 매 10분마다 동기화
	 * - 최종 스코어 및 상태 확정
	 */
	@Scheduled(fixedDelay = 600000)
	public void syncEndedMatches(){
		log.info("=== [ENDED] 종료 경기 동기화 시작 ===");
		try{
			int count = matchSyncService.syncEndedMatches(FOOTBALL, null);
			log.info("[ENDED] 동기화 완료 - {}건", count);
		} catch (Exception e) {
			log.error("[ENDED] 동기화 실패", e);
		}
	}

}
