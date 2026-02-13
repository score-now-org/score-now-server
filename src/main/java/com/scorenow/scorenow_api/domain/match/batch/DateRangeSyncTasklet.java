package com.scorenow.scorenow_api.domain.match.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.TaskExecutor;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class DateRangeSyncTasklet implements Tasklet {

	protected static final String FOOTBALL = "1";
	protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	protected final MatchSyncService matchSyncService;
	protected final TaskExecutor taskExecutor;
	protected final int days;

	protected abstract String getLogLabel();
	protected abstract LocalDate getTargetDate(LocalDate base, int offset);
	protected abstract int syncForDate(String date);

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		log.info("=== [BATCH] {} 동기화 시작 ({}일 범위) ===", getLogLabel(), days);

		LocalDate today = LocalDate.now();
		AtomicInteger totalCount = new AtomicInteger(0);
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i = 0; i <= days; i++) {
			final String date = getTargetDate(today, i).format(DATE_FORMAT);

			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					int count = syncForDate(date);
					totalCount.addAndGet(count);
					log.debug("[BATCH] {} {} 완료: {}건", getLogLabel(), date, count);
				} catch (Exception e) {
					log.error("[BATCH] {} {} 실패: {}", getLogLabel(), date, e.getMessage());
				}
			}, taskExecutor);

			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		log.info("=== [BATCH] {} 동기화 완료 - 총 {}건 ===", getLogLabel(), totalCount.get());
		return RepeatStatus.FINISHED;
	}
}
