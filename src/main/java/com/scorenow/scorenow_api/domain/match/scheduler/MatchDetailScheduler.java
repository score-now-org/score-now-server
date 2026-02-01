package com.scorenow.scorenow_api.domain.match.scheduler;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchDetailScheduler {

    private final MatchRepository matchRepository; // MySQL
    private final MatchDetailService matchDetailService;

    @Scheduled(fixedDelay = 5000) // 일단 짧게 테스트
    public void syncInplayDetails() {
        log.info(">>>>>> 스케줄러 실행 확인 <<<<<<");
        List<Match> inplayMatches = matchRepository.findByStatusCode(MatchStatus.IN_PLAY);
        log.info("조회된 In-play 경기 수: {}", inplayMatches.size());

        for (Match match : inplayMatches) {
            log.info("상세 업데이트 시작 - 경기 ID: {}", match.getId());
            matchDetailService.updateInplayMatchDetail(match.getId());
        }
    }
}