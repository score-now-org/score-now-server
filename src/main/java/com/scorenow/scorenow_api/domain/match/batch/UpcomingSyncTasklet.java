package com.scorenow.scorenow_api.domain.match.batch;

import java.time.LocalDate;

import org.springframework.core.task.TaskExecutor;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;

public class UpcomingSyncTasklet extends DateRangeSyncTasklet {

	public UpcomingSyncTasklet(MatchSyncService matchSyncService, TaskExecutor taskExecutor, int days) {
		super(matchSyncService, taskExecutor, days);
	}

	@Override
	protected String getLogLabel() {
		return "Upcoming";
	}

	@Override
	protected LocalDate getTargetDate(LocalDate base, int offset) {
		return base.plusDays(offset);
	}

	@Override
	protected int syncForDate(String date) {
		return matchSyncService.syncUpcomingMatches(FOOTBALL, date);
	}
}