package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballMatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.SportDetailNormalizerRegistry;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.stage.MatchStageResponseResolver;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.StadiumData;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.ViewResult;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {
    private final StadiumCacheService stadiumCacheService;

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    private final MatchRealtimeEventPublisher eventPublisher;

    private final SportDetailNormalizerRegistry normalizerRegistry;
    private final SportDetailEventProcessorRegistry eventProcessorRegistry;

    private final MatchStageResponseResolver matchStageResponseResolver;

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetail(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));

        return MatchDetailResponse.builder()
                .startAt(match.getStartAt())
                .statusCode(match.getStatusCode().name())
                .statusName(match.getStatusCode().getDescription())
                .homeScore(matchDetailDocument.getHomeScore())
                .awayScore(matchDetailDocument.getAwayScore())
                .currentCommentary(matchDetailDocument.getCurrentCommentary())
                .currentCommentaryHighlighted(matchDetailDocument.isCurrentCommentaryHighlighted())
                .build();
    }

    @Transactional
    public void updateInplayMatchDetail(DataOrigin dataOrigin, ViewResult viewResult) {
        String apiMatchId = viewResult.getId();

        Match match = matchRepository.findByExternalInfo(dataOrigin, apiMatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchDetailDocument currentMatchDetailDocument = matchDetailRepository.findById(match.getId())
                .orElse(MatchDetailDocument.builder().build());

        // 종목에 특화된 형태로 정규화
        SportDetailType sportDetailType = SportDetailType.fromSportId(match.getSportId());
        SportDetail sportDetail = normalizerRegistry.normalize(sportDetailType, viewResult);

        // 새로운 문서 생성
        MatchDetailDocument newMatchDetailDocument = MatchDetailDocument.builder()
                .id(match.getId())
                .type(sportDetailType)
                .homeScore(viewResult.getHomeScore())
                .awayScore(viewResult.getAwayScore())
                .sportDetail(sportDetail)
                .build();

        // 종목 공통 : 점수 업데이트 및 이벤트 발행
        // TODO: 이거 이상한지 확인해봐야할듯.
        updateMatchScoreAndPublishEvent(match, newMatchDetailDocument.getHomeScore(), newMatchDetailDocument.getAwayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        StadiumData stadiumData = viewResult.getExtra().getStadiumData();
        if (stadiumData != null) {
            Stadium stadium = stadiumCacheService.getOrCreateStadium(
                    dataOrigin,
                    viewResult.getSportId(),
                    stadiumData.getId(),
                    stadiumData.getName(),
                    stadiumData.getCity());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (match.isStadiumEmpty()) {
                match.updateStadiumId(stadium.getId());
            }
        }

        // 종목 상세 : 종목별 상세 정보 업데이트 및 이벤트 발행
        eventProcessorRegistry.process(currentMatchDetailDocument, newMatchDetailDocument);

        // 경기 상세 정보 upsert
        matchDetailRepository.upsertMatchDetail(newMatchDetailDocument);

        // 종목 공통 : 경기 상태 업데이트 및 이벤트 발행
        updateMatchStatusAndPublishEvent(
                match,
                newMatchDetailDocument,
                MatchStatus.fromCode(viewResult.getTimeStatus())
        );

        log.info("✅ 경기 상세 데이터(MySQL & MongoDB) 동기화 완료 matchId:{}, apiMatchId:{}", match.getId(), apiMatchId);
    }

    /**
     * 어드민 수동 수정 (MySQL 스코어 + MongoDB 상세 데이터)
     * 참고) 자동 경기, 수동 경기 모두 수동 수정이 가능하다.
     */
    @Transactional
    public void updateMatchDetailManual(Long matchId, FootballMatchDetailUpdateRequest request) {
        // 경기 정보 조회
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 경기 상세 정보 조회
        MatchDetailDocument currentMatchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 축구 경기 검증
        if (currentMatchDetailDocument.getType() != SportDetailType.FOOTBALL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 경기 상세 정보 수정 요청이 아닙니다.");
        }

        // 업데이트 요청 정보와 기존 경기 상세 정보 기반으로 새로운 객체 생성
        MatchDetailDocument newMatchDetailDocument = applyFootballDetailUpdateRequest(currentMatchDetailDocument, request);

        // 공통 : 점수 업데이트 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, request.getHomeScore(), request.getAwayScore());

        // TODO: 경기 시작 시간 변경이 있을 수 있으므로 해당 부분도 처리해야 한다.
        //  (경기 시작을 변경하면 추후 자동 동기화의 경우 경기 시작시간 업데이트되는지)
        if (request.getStartAt() != null) {
            match.updateStartAt(request.getStartAt());
        }

        // 경기 상세 정보 저장
        matchDetailRepository.upsertMatchDetail(newMatchDetailDocument);

        // 종목 상세 : 종목별 상세 정보 업데이트 및 이벤트 발행
        eventProcessorRegistry.process(currentMatchDetailDocument, newMatchDetailDocument);

        // 공통 : 경기 상태 업데이트 및 이벤트 발행
        if (request.getStatus() != null) {
            updateMatchStatusAndPublishEvent(match, newMatchDetailDocument, request.getStatus());
        }
    }

    private MatchDetailDocument applyFootballDetailUpdateRequest(
            MatchDetailDocument currentMatchDetailDocument,
            FootballMatchDetailUpdateRequest request) {

        FootballDetail currentFootballDetail = resolveFootballDetail(currentMatchDetailDocument.getSportDetail());

        FootballDetail newFootballDetail = FootballDetail.builder()
                .shootOutScore(resolveShootOutScore(currentFootballDetail.getShootOutScore(), request))
                .clock(resolveClock(currentFootballDetail.getClock(), request))
                .additionalTime(resolveAdditionalTime(currentFootballDetail.getAdditionalTime(), request))
                .homeStats(resolveStats(currentFootballDetail.getHomeStats(), request.getHomeStats()))
                .awayStats(resolveStats(currentFootballDetail.getAwayStats(), request.getAwayStats()))
                .build();

        return MatchDetailDocument.builder()
                .id(currentMatchDetailDocument.getId())
                .type(currentMatchDetailDocument.getType())
                .homeScore(request.getHomeScore() != null ? request.getHomeScore() : currentMatchDetailDocument.getHomeScore())
                .awayScore(request.getAwayScore() != null ? request.getAwayScore() : currentMatchDetailDocument.getAwayScore())
                .currentCommentaryId(currentMatchDetailDocument.getCurrentCommentaryId())
                .currentCommentary(currentMatchDetailDocument.getCurrentCommentary())
                .currentCommentaryHighlighted(currentMatchDetailDocument.isCurrentCommentaryHighlighted())
                .sportDetail(newFootballDetail)
                .build();
    }

    /**
     * 점수에 변동 사항이 있는 경우 점수 업데이트 진행 및 이벤트 발행
     */
    private void updateMatchScoreAndPublishEvent(
            Match match,
            Integer newHomeScore,
            Integer newAwayScore) {

        // 정규시간 점수 변경
        boolean scoreChanged = false;

        if (newHomeScore != null && !newHomeScore.equals(match.getHomeScore())) {
            match.updateHomeScore(newHomeScore);
            scoreChanged = true;
        }

        if (newAwayScore != null && !newAwayScore.equals(match.getAwayScore())) {
            match.updateAwayScore(newAwayScore);
            scoreChanged = true;
        }

        if (scoreChanged) {
            eventPublisher.publishScoreChanged(
                    match.getId(),
                    match.getHomeScore(),
                    match.getAwayScore());
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

        MatchStageResponse matchStageResponse = matchStageResponseResolver.resolve(match, matchDetailDocument);

        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, matchStageResponse.getDisplayText());
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

    private FootballStats resolveStats(FootballStats currentStats, FootballMatchDetailUpdateRequest.FootballStats request) {
        if (request == null) {
            return currentStats;
        }

        if (currentStats == null) {
            currentStats = FootballStats.empty();
        }

        return FootballStats.builder()
                .yellowCards(request.getYellowCards() != null ? request.getYellowCards() : currentStats.getYellowCards())
                .redCards(request.getRedCards() != null ? request.getRedCards() : currentStats.getRedCards())
                .shots(request.getShots() != null ? request.getShots() : currentStats.getShots())
                .shotsOnTarget(request.getShotsOnTarget() != null ? request.getShotsOnTarget() : currentStats.getShotsOnTarget())
                .possession(request.getPossession() != null ? request.getPossession() : currentStats.getPossession())
                .offsides(request.getOffsides() != null ? request.getOffsides() : currentStats.getOffsides())
                .fouls(request.getFouls() != null ? request.getFouls() : currentStats.getFouls())
                .corners(request.getCorners() != null ? request.getCorners() : currentStats.getCorners())
                .freeKicks(request.getFreeKicks() != null ? request.getFreeKicks() : currentStats.getFreeKicks())
                .build();
    }

    private FootballAdditionalTime resolveAdditionalTime(
            FootballAdditionalTime currentAdditionalTime,
            FootballMatchDetailUpdateRequest request) {

        if (currentAdditionalTime == null) {
            currentAdditionalTime = FootballAdditionalTime.builder().build();
        }

        return FootballAdditionalTime.builder()
                .firstHalf(request.getFirstHalf() != null ? request.getFirstHalf() : currentAdditionalTime.getFirstHalf())
                .secondHalf(request.getSecondHalf() != null ? request.getSecondHalf() : currentAdditionalTime.getSecondHalf())
                .firstHalf(request.getExtraFirstHalf() != null ? request.getExtraFirstHalf() : currentAdditionalTime.getExtraFirstHalf())
                .firstHalf(request.getExtraSecondHalf() != null ? request.getExtraSecondHalf() : currentAdditionalTime.getExtraSecondHalf())
                .build();
    }

    private FootballClock resolveClock(FootballClock currentClock, FootballMatchDetailUpdateRequest request) {
        if (currentClock == null) {
            currentClock = FootballClock.empty();
        }

        return FootballClock.builder()
                .elapsedMinutes(currentClock.getElapsedMinutes())
                .elapsedSeconds(currentClock.getElapsedSeconds())
                .phase(request.getPhase() != null ? request.getPhase() : currentClock.getPhase())
                .running(currentClock.getRunning())
                .providerUpdatedAt(currentClock.getProviderUpdatedAt())
                .build();
    }

    private FootballShootOutScore resolveShootOutScore(
            FootballShootOutScore currentShootOutScore,
            FootballMatchDetailUpdateRequest request) {

        if (currentShootOutScore == null) {
            currentShootOutScore = FootballShootOutScore.builder().build();
        }

        return FootballShootOutScore.builder()
                .homeScore(request.getHomeScore() != null ? request.getHomeScore() : currentShootOutScore.getHomeScore())
                .awayScore(request.getAwayScore() != null ? request.getAwayScore() : currentShootOutScore.getAwayScore())
                .build();
    }
}

