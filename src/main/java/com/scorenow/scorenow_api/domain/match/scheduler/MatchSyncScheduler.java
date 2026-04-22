package com.scorenow.scorenow_api.domain.match.scheduler;

import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSyncScheduler {

	private static final String FOOTBALL = "1";

	private final JobLauncher jobLauncher;
	private final Job matchSyncJob;
	private final MatchSyncService matchSyncService;

	/**
	 * 매일 2회 배치 실행(4시, 16시)
	 */
	@Scheduled(cron = "0 0 4,16 * * *")
	public void runMatchSyncJob() {
		log.info("=== Match Sync Batch Job 시작 ===");

		try {
			JobParameters params = new JobParametersBuilder()
				.addString("runTime", LocalDateTime.now().toString())
				.toJobParameters();

			jobLauncher.run(matchSyncJob, params);

			log.info("=== Match Sync Batch Job 완료 ===");
		} catch (Exception e) {
			log.error("Batch Job 실패", e);
		}
	}

	/**
	 * Ended - 10분마다 (오늘 경기만, 종료 후 30분 내 반영)
	 */
	@Scheduled(fixedDelay = 600000)
	public void syncEndedMatches() {
		log.info("=== [ENDED] 종료 경기 동기화 ===");

		try {
			int count = matchSyncService.syncEndedMatches(FOOTBALL, null);
			log.info("[ENDED] 동기화 완료 - {}건", count);
		} catch (Exception e) {
			log.error("[ENDED] 동기화 실패", e);
		}
	}

}
