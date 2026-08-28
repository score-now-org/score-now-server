package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import com.scorenow.scorenow_api.domain.match.dto.request.*;
import com.scorenow.scorenow_api.domain.match.dto.response.FootballAdminMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.FootballMatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FootballAdminMatchDetailService {

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final FootballMatchDetailRepository footballMatchDetailRepository;

    private final MatchRealtimeEventPublisher eventPublisher;
    private final SportDetailEventProcessorRegistry eventProcessorRegistry;

    private final MatchStatusDisplayResolver matchStatusDisplayResolver;
    private final MatchScheduledStartAtUpdater matchScheduledStartAtUpdater;

    /**
     * 축구 경기 중계 데이터 조회
     */
    @Transactional(readOnly = true)
    public FootballAdminMatchDetailResponse getFootballMatchDetail(Long matchId) {

        Match match = findMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);

        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());

        return FootballAdminMatchDetailResponse.builder()
                .startAt(match.getStartAt())
                .statusCode(match.getStatusCode().name())
                .statusName(match.getStatusCode().getDescription())
                .homeScore(match.getHomeScore())
                .awayScore(match.getAwayScore())
                .currentCommentary(matchDetailDocument.getCurrentCommentary())
                .currentCommentaryHighlighted(matchDetailDocument.isCurrentCommentaryHighlighted())
                .footballDetail(FootballAdminMatchDetailResponse.FootballDetailResponse.from(footballDetail))
                .build();
    }

    /**
     * 경기 예정 시각 수정
     */
    @Transactional
    public void updateMatchScheduledStartAt(Long matchId, MatchScheduledStartAtUpdateRequest request) {

        Match match = findMatchByMatchId(matchId);

        matchScheduledStartAtUpdater.update(match, request.getStartAt());
    }

    /**
     * 경기 상태 수정
     */
    @Transactional
    public void updateMatchStatus(Long matchId, MatchStatusUpdateRequest request) {

        Match match = findMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);

        updateMatchStatusAndPublishEvent(match, matchDetailDocument, request.getStatus());
    }

    /**
     * 점수 수정
     */
    @Transactional
    public void updateMatchScore(Long matchId, MatchScoreUpdateRequest request) {

        Match match = findMatchByMatchId(matchId);

        boolean scoreChanged = !Objects.equals(match.getHomeScore(), request.getHomeScore())
                || !Objects.equals(match.getAwayScore(), request.getAwayScore());

        if (!scoreChanged) {
            return;
        }

        match.updateHomeScore(request.getHomeScore());
        match.updateAwayScore(request.getAwayScore());

        eventPublisher.publishScoreChanged(matchId, request.getHomeScore(), request.getAwayScore());
    }

    /**
     * 추가시간 수정
     */
    @Transactional
    public void updateAdditionalTime(Long matchId, FootballAdditionalTimeUpdateRequest request) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);

        resolveFootballDetail(matchDetailDocument.getSportDetail());

        Integer firstHalf = request.getFirstHalf();
        Integer secondHalf = request.getSecondHalf();
        Integer extraFirstHalf = request.getExtraFirstHalf();
        Integer extraSecondHalf = request.getExtraSecondHalf();

        FootballAdditionalTime newAdditionalTime = FootballAdditionalTime.of(
                firstHalf,
                secondHalf,
                extraFirstHalf,
                extraSecondHalf
        );

        footballMatchDetailRepository.updateAdditionalTime(matchId, newAdditionalTime);
    }

    /**
     * 경기 스텟 수정
     */
    @Transactional
    public void updateFootballStats(Long matchId, FootballStatsUpdateRequest request) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);

        resolveFootballDetail(matchDetailDocument.getSportDetail());

        FootballStats newHomeStats = request.getHomeStats().toFootballStats();
        FootballStats newAwayStats = request.getAwayStats().toFootballStats();

        footballMatchDetailRepository.updateStats(matchId, newHomeStats, newAwayStats);
    }

    /**
     * 승부차기 점수 수정
     */
    @Transactional
    public void updateShootOutScore(Long matchId, FootballShootOutScoreUpdateRequest request) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);
        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());

        FootballDetail newFootballDetail = footballDetail.withShootOutScore(request.toFootballShootOutScore());
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newFootballDetail);

        footballMatchDetailRepository.updateShootOutScore(matchId, newFootballDetail.getShootOutScore());

        // 승부차기 점수 변경 이벤트 발행
        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 축구 phase 전환
     * 전반전 시작, 경기종료 => 경기 상태 변경 필요
     * 그 외 => 진행중 상태 검증 필요
     */
    @Transactional
    public void updatePhase(Long matchId, FootballPhaseTransitionRequest request) {

        Match match = findMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);

        validateFootballMatch(match, matchDetailDocument);

        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());
        FootballPhase newPhase = request.getFootballPhase();

        validatePhaseTransition(newPhase, match.getStatusCode());

        FootballDetail newFootballDetail = footballDetail.withFootballClock(FootballClock.startPhase(newPhase));
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newFootballDetail);

        footballMatchDetailRepository.updateClock(matchId, newFootballDetail.getClock());

        // Phase 변경 이벤트 발행
        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);

        if (newPhase == FootballPhase.FIRST_HALF) {
            updateMatchStatusAndPublishEvent(match, newMatchDetailDocument, MatchStatus.IN_PLAY);
        }

        if (newPhase == FootballPhase.FULL_TIME) {
            updateMatchStatusAndPublishEvent(match, newMatchDetailDocument, MatchStatus.ENDED);
        }
    }

    /**
     * 축구 시간 보정
     */
    @Transactional
    public void correctClock(Long matchId, FootballClockCorrectionRequest request) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);

        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());
        FootballClock clock = resolveRequiredClock(footballDetail);

        FootballClock correctedClock = clock.correctPhaseElapsedSeconds(
                request.getElapsedMinutes(),
                request.getElapsedSeconds()
        );

        footballMatchDetailRepository.updateClock(matchId, correctedClock);
    }

    /**
     * 경기 시각 일시정지
     */
    @Transactional
    public void pauseClock(Long matchId) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);

        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());
        FootballClock clock = resolveRequiredClock(footballDetail);

        if (Boolean.FALSE.equals(clock.getRunning())) {
            return;
        }

        FootballClock newClock = clock.pause();
        FootballDetail newFootballDetail = footballDetail.withFootballClock(newClock);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newFootballDetail);

        footballMatchDetailRepository.updateClock(matchId, newClock);

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 경기 시각 재시작
     */
    @Transactional
    public void resumeClock(Long matchId) {

        MatchDetailDocument matchDetailDocument = findFootballMatchDetailDocumentByMatchId(matchId);

        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument.getSportDetail());
        FootballClock clock = resolveRequiredClock(footballDetail);

        if (Boolean.TRUE.equals(clock.getRunning())) {
            return;
        }

        FootballClock newClock = clock.resume();
        FootballDetail newFootballDetail = footballDetail.withFootballClock(newClock);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newFootballDetail);

        footballMatchDetailRepository.updateClock(matchId, newClock);

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    private Match findMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private MatchDetailDocument findMatchDetailDocumentByMatchId(Long matchId) {
        return matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));
    }

    private MatchDetailDocument findFootballMatchDetailDocumentByMatchId(Long matchId) {
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);

        if (matchDetailDocument.getType() != SportDetailType.FOOTBALL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 경기 상세 정보가 아닙니다.");
        }

        return matchDetailDocument;
    }

    private FootballDetail resolveFootballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return FootballDetail.empty();
        }

        if (!(sportDetail instanceof FootballDetail footballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 상세 정보가 아닙니다.");
        }

        return footballDetail;
    }

    /**
     * FootballClock 이 존재하는지 검증
     */
    private FootballClock resolveRequiredClock(FootballDetail footballDetail) {
        FootballClock clock = footballDetail.getClock();
        if (clock == null
                || clock.getPhase() == null
                || clock.getClockSyncedAt() == null
                || clock.getPhaseElapsedSeconds() == null
                || clock.getRunning() == null) {
            throw new BusinessException(ErrorCode.MATCH_CLOCK_NOT_INITIALIZED);
        }

        return clock;
    }

    private void validateFootballMatch(Match match, MatchDetailDocument currentMatchDetailDocument) {
        // 경기 종목 사전 검증
        SportDetailType matchSportDetailType = SportDetailType.fromSportId(match.getSportId());
        if (matchSportDetailType != SportDetailType.FOOTBALL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 경기 상세 정보 수정 요청이 아닙니다.");
        }

        // 축구 경기 검증
        if (currentMatchDetailDocument.getType() != SportDetailType.FOOTBALL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 경기 상세 정보 수정 요청이 아닙니다.");
        }
    }

    /**
     * 경기 상태 업데이트 및 이벤트 발행
     */
    private void updateMatchStatusAndPublishEvent(
            Match match,
            MatchDetailDocument matchDetailDocument,
            MatchStatus newStatus) {

        if (Objects.equals(match.getStatusCode(), newStatus)) {
            return;
        }

        match.updateStatus(newStatus);

        MatchStatusDisplayResponse matchStatusDisplayResponse = matchStatusDisplayResolver.resolve(match, matchDetailDocument);

        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, matchStatusDisplayResponse.getDisplayText());
    }

    /**
     * Phase 전환 시, 전반전 시작이나 경기 종료가 아닌 나머지 Phase 의 경우 기존 경기 상태가 IN_PLAY 인지 검증한다.
     */
    private void validatePhaseTransition(FootballPhase newPhase, MatchStatus currentStatus) {

        boolean isInplayPhase = newPhase != FootballPhase.FIRST_HALF && newPhase != FootballPhase.FULL_TIME;

        if (isInplayPhase && currentStatus != MatchStatus.IN_PLAY) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS, "진행중인 경기가 아닙니다.");
        }
    }

}
