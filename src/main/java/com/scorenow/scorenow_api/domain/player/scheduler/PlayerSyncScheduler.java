package com.scorenow.scorenow_api.domain.player.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerSyncScheduler {
	private final JobLauncher jobLauncher;
	private final Job playerSyncJob;

	@Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
	public void runPlayerSyncJob() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("runType", "SCHEDULED")
			.addLong("time", System.currentTimeMillis())
			.toJobParameters();

		jobLauncher.run(playerSyncJob, jobParameters);

		log.info("=== Player Sync Batch Job 실행 완료 ===");
	}
}
