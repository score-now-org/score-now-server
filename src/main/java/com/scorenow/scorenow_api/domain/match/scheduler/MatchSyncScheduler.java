package com.scorenow.scorenow_api.domain.match.scheduler;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.scorenow.scorenow_api.domain.match.service.InplayMatchCandidateLoadService;
import com.scorenow.scorenow_api.domain.match.service.InplayMatchSyncService;
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

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE_ID;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSyncScheduler {

    private static final String FOOTBALL = "1";

    private final JobLauncher jobLauncher;
    private final Job matchSyncJob;
    private final MatchSyncService matchSyncService;

    private final InplayMatchCandidateLoadService inplayMatchCandidateLoadService;
    private final InplayMatchSyncService inplayMatchSyncService;

    /**
     * 매일 2회 배치 실행(4시, 16시)
     * 테스트 시 임시 사용: @Scheduled(initialDelay = 10000, fixedRate = 1000000)
     */
    @Scheduled(cron = "0 0 4,16 * * *", zone = "Asia/Seoul")
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
    @Scheduled(fixedDelay = 600000, zone = "Asia/Seoul")
    public void syncEndedMatches() {
        log.info("=== [ENDED] 종료 경기 동기화 ===");

        try {
            int count = matchSyncService.syncEndedMatches(FOOTBALL, null);
            log.info("[ENDED] 동기화 완료 - {}건", count);
        } catch (Exception e) {
            log.error("[ENDED] 동기화 실패", e);
        }
    }

    /**
     * 작업 내용 : 현재 시간 기준 향후 1시간 이내 시작 예정인 경기들을 조회 후 캐싱
     * 작업 주기 : 매 시 정각과 30분
     */
    @Scheduled(cron = "0 0/30 * * * *", zone = "Asia/Seoul")
    public void loadInplayCandidatesToCache() {
        log.info("📅 INPLAY 예정 경기 캐싱 시작");

        try {
            inplayMatchCandidateLoadService.loadInplayCandidatesToCache(ZonedDateTime.now(SEOUL_TIME_ZONE_ID));
        } catch (Exception e) {
            log.error("시작 예정 경기 캐싱 작업 중 예외 발생 ❌", e);
        }
    }

    /**
     * 작업 내용 : Redis ZSet 에 저장한 1시간 이내 시작 예정인 경기 중, 경기 시작한 건에 대해서 외부 API 를 호출을 통한 동기화 작업을 진행
     * 작업 주기 : 매 분 (1분 마다)
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void syncInplayMatches() {
        log.info("📅 INPLAY 경기 동기화 시작");

        try {
            inplayMatchSyncService.syncInplayMatches(ZonedDateTime.now(SEOUL_TIME_ZONE_ID));
        } catch (Exception e) {
            log.error("INPLAY 경기 동기화 처리 중 예외 발생 ❌", e);
        }
    }

}
