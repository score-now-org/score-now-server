package com.scorenow.scorenow_api.domain.match.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MatchSyncJobConfig {

	private static final int UPCOMING_DAYS = 14;
	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final MatchSyncService matchSyncService;

	@Bean
	public Job matchSyncJob(
		@Qualifier("upcomingSyncStep") Step upcomingSyncStep
	) {
		return new JobBuilder("matchSyncJob", jobRepository)
			.start(upcomingSyncStep)
			.build();
	}

	@Bean
	public Step upcomingSyncStep(@Qualifier("batchTaskExecutor") TaskExecutor taskExecutor) {
		return new StepBuilder("upcomingSyncStep", jobRepository)
			.tasklet(new UpcomingSyncTasklet(matchSyncService, taskExecutor, UPCOMING_DAYS), transactionManager)
			.build();
	}
}