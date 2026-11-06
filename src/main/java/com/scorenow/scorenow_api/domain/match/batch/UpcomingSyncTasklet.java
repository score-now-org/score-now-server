package com.scorenow.scorenow_api.domain.match.batch;

import java.time.LocalDate;

import org.springframework.core.task.TaskExecutor;

import com.scorenow.scorenow_api.domain.match.service.UpcomingMatchSyncService;

public class UpcomingSyncTasklet extends DateRangeSyncTasklet {

	private static final String UPCOMING = "예정 경기 동기화";
	private final UpcomingMatchSyncService upcomingMatchSyncService;

	public UpcomingSyncTasklet(
		UpcomingMatchSyncService upcomingMatchSyncService,
		TaskExecutor taskExecutor,
		int days
	) {
		super(taskExecutor, days);
		this.upcomingMatchSyncService = upcomingMatchSyncService;
	}

	@Override
	protected String getLogLabel() {
		return UPCOMING;
	}

	@Override
	protected LocalDate getTargetDate(LocalDate base, int offset) {
		return base.plusDays(offset);
	}

	@Override
	protected int syncForDate(LocalDate localDate) {
		return upcomingMatchSyncService.sync(localDate);
	}
}
