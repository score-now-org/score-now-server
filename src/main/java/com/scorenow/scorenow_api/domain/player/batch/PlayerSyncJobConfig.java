package com.scorenow.scorenow_api.domain.player.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.scorenow.scorenow_api.domain.player.batch.dto.TeamPlayerSyncData;
import com.scorenow.scorenow_api.domain.player.batch.dto.TeamPlayerSyncItem;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PlayerSyncJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	// private final PlayerSyncTasklet playerSyncTasklet;
	private final TeamPlayerSyncItemReader reader;
	private final TeamPlayerSyncProcessor processor;
	private final TeamPlayerSyncWriter writer;

	@Bean
	public Job playerSyncJob(Step playerSyncStep) {
		return new JobBuilder("playerSyncJob", jobRepository)
			.start(playerSyncStep)
			.build();
	}
	
	@Bean
	public Step playerSyncStep() {
		return new StepBuilder("playerSyncStep", jobRepository)
			.<TeamPlayerSyncItem, TeamPlayerSyncData>chunk(10, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}
}
