package com.scorenow.scorenow_api.domain.player.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlayerSyncService {

	private final JobLauncher jobLauncher;
	private final Job playerSyncJob;

	public void runPlayerSyncJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder()
				.addString("runType", "MANUAL")
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();

			jobLauncher.run(playerSyncJob, jobParameters);
		} catch (Exception e) {
			log.error("선수 동기화 배치 실행 실패", e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "선수 동기화 배치 실행에 실패했습니다.");
		}
	}
}
