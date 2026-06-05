package com.scorenow.scorenow_api.domain.match.scheduler;

import com.scorenow.scorenow_api.domain.match.service.MatchDetailSyncService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchDetailScheduler {

    private final MatchDetailSyncService matchDetailSyncService;

    /**
     * 작업 내용 : 현재 진행중인 경기의 세부 정보를 동기화 한다. (세부 정보 : 경기 중 발생한 이벤트 기반으로 수치화된 스텟 정보들)
     * 작업 주기 : 1분 마다
     */
    @Scheduled(fixedDelay = 60000, zone = "Asia/Seoul")
    // TODO: 작업 주기는 동적으로 관리될 필요가 있을 듯 하고, 추가적으로 API 리밋도 같이 고려하는 주기로 설정해야 한다.
    public void syncInplayMatchDetails() {
        log.info("📅 INPLAY 경기 세부 정보 동기화 시작");

        try {
            matchDetailSyncService.syncInplayMatchDetails();
        } catch (Exception e) {
            log.error("INPLAY 경기 세부 정보 동기화 작업 중 예외 발생 ❌", e);
        }
    }
}