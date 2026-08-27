package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.service.displaytext.football.FootballClockDisplayTextResolver;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FootballDetailEventProcessor implements SportDetailEventProcessor {

    private final MatchRealtimeEventPublisher eventPublisher;
    private final FootballClockDisplayTextResolver footballClockDisplayTextResolver;

    @Override
    public SportDetailType type() {
        return SportDetailType.FOOTBALL;
    }

    /**
     * 축구 경기에 대해 이벤트 발행 처리를 담당하는 메서드
     *  - 승부차기 스코어 변화
     *  - 경기 시간 정보 변화 (phase 변화, 경기 running 여부 변화)
     */
    @Override
    public void process(MatchDetailDocument currentMatchDetailDocument, MatchDetailDocument newMatchDetailDocument) {

        // SportDetail 이 축구인지 검증 및 타입 캐스팅
        FootballDetail currentFootballDetail = resolveFootballDetail(currentMatchDetailDocument.getSportDetail());
        FootballDetail newFootballDetail = resolveFootballDetail(newMatchDetailDocument.getSportDetail());

        Long matchId = newMatchDetailDocument.getId();

        // 경기 시간 정보 반영 (경기 시간 정보가 제공되는 경우에만 / 경기전,경기종료의 경우 시간 정보가 제공되지 않는다.)
        processClock(matchId, currentFootballDetail.getClock(), newFootballDetail.getClock());

        // 승부차기 정보 반영
        processPenaltyShootOut(matchId, currentFootballDetail.getShootOutScore(), newFootballDetail.getShootOutScore());
    }

    private void processClock(Long matchId, FootballClock currentClock, FootballClock newClock) {

        // 1. 새로운 경기 시간 정보가 없다면 동기화 할 필요가 없으니 종료
        if (newClock == null) {
            return;
        }

        // 2. 이벤트 발행 여부 확인 후 발행
        if (shouldPublishEvent(currentClock, newClock)) {
            String displayText = footballClockDisplayTextResolver.resolve(newClock);
            eventPublisher.publishFootballClockChanged(matchId, newClock, displayText);
        }

    }

    /**
     * [이벤트 발행 조건]
     * 1. 기존 경기 시간 정보가 없었던 경우 이벤트 발행
     * 2. 경기 구간 (phase) 또는 타이머 진행 여부 (running) 변경 시 이벤트 발행
     */
    private boolean shouldPublishEvent(FootballClock currentClock, FootballClock newClock) {

        if (currentClock == null) {
            return true;
        }

        return !Objects.equals(currentClock.getPhase(), newClock.getPhase()) ||
                !Objects.equals(currentClock.getRunning(), newClock.getRunning());
    }

    private void processPenaltyShootOut(
            Long matchId,
            FootballShootOutScore currentShootOutScore,
            FootballShootOutScore newShootOutScore) {

        if (newShootOutScore == null) {
            return;
        }

        if (currentShootOutScore == null || currentShootOutScore.isShootOutScoreChanged(newShootOutScore)) {
            eventPublisher.publishFootballShootOutScoreChanged(matchId, newShootOutScore);
        }
    }

    private static FootballDetail resolveFootballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return FootballDetail.empty();
        }

        if (!(sportDetail instanceof FootballDetail footballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 상세 정보가 아닙니다.");
        }

        return footballDetail;
    }
}
