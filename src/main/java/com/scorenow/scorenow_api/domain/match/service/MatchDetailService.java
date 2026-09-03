package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.SportDetailNormalizerRegistry;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
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

    private final MatchStatusDisplayResolver matchStatusDisplayResolver;

    @Transactional
    public void updateInplayMatchDetail(DataOrigin dataOrigin, ViewResult viewResult) {
        String apiMatchId = viewResult.getId();

        Match match = matchRepository.findByExternalInfo(dataOrigin, apiMatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchDetailDocument currentMatchDetailDocument = matchDetailRepository.findById(match.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));

        SportDetailType sportDetailType = SportDetailType.fromSportId(match.getSportId());

        if (currentMatchDetailDocument.getType() != sportDetailType) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "경기 상세 종목 정보가 일치하지 않습니다.");
        }

        // 종목에 특화된 형태로 정규화
        SportDetail normalizedSportDetail = normalizerRegistry.normalize(sportDetailType, viewResult);

        // 기존 종목 상세 정보와 정규화한 상세 정보를 비교하여 병합
        SportDetail mergedSportDetail = mergeSportDetail(
                sportDetailType,
                currentMatchDetailDocument.getSportDetail(),
                normalizedSportDetail
        );

        // 새로운 문서 생성
        MatchDetailDocument newMatchDetailDocument = MatchDetailDocument.builder()
                .id(match.getId())
                .type(sportDetailType)
                .sportDetail(mergedSportDetail)
                .build();

        // 종목 공통 : 점수 업데이트 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, viewResult.getHomeScore(), viewResult.getAwayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        StadiumData stadiumData = viewResult.getExtra().getStadiumData();
        if (stadiumData != null) {
            Long stadiumId = stadiumCacheService.getOrCreateStadiumId(
                    dataOrigin,
                    viewResult.getSportId(),
                    stadiumData.getId(),
                    stadiumData.getName(),
                    stadiumData.getCity());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (match.isStadiumEmpty()) {
                match.updateStadiumId(stadiumId);
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

    private SportDetail mergeSportDetail(
            SportDetailType sportDetailType,
            SportDetail currentSportDetail,
            SportDetail newSportDetail) {

        return switch (sportDetailType) {
            case FOOTBALL -> mergeFootballDetail(currentSportDetail, newSportDetail);
            default -> newSportDetail;
        };
    }

    private FootballDetail mergeFootballDetail(SportDetail currentSportDetail, SportDetail newSportDetail) {
        FootballDetail currentFootballDetail = resolveFootballDetail(currentSportDetail);
        FootballDetail newFootballDetail = resolveFootballDetail(newSportDetail);


        return FootballDetail.builder()
                .shootOutScore(resolveIncomingOrCurrent(currentFootballDetail.getShootOutScore(), newFootballDetail.getShootOutScore()))
                .clock(resolveIncomingOrCurrent(currentFootballDetail.getClock(), newFootballDetail.getClock()))
                .additionalTime(mergeAdditionalTime(currentFootballDetail.getAdditionalTime(), newFootballDetail.getAdditionalTime()))
                .homeStats(resolveIncomingOrCurrent(currentFootballDetail.getHomeStats(), newFootballDetail.getHomeStats()))
                .awayStats(resolveIncomingOrCurrent(currentFootballDetail.getAwayStats(), newFootballDetail.getAwayStats()))
                .build();
    }

    /**
     * additionalTime은 구간별로 일부 값만 내려오므로 기존 값과 필드 단위로 병합한다.
     */
    private FootballAdditionalTime mergeAdditionalTime(
            FootballAdditionalTime currentAdditionalTime,
            FootballAdditionalTime newAdditionalTime) {

        if (currentAdditionalTime == null) {
            return newAdditionalTime;
        }

        if (newAdditionalTime == null) {
            return currentAdditionalTime;
        }

        return FootballAdditionalTime.builder()
                .firstHalf(resolveIncomingOrCurrent(currentAdditionalTime.getFirstHalf(), newAdditionalTime.getFirstHalf()))
                .secondHalf(resolveIncomingOrCurrent(currentAdditionalTime.getSecondHalf(), newAdditionalTime.getSecondHalf()))
                .extraFirstHalf(resolveIncomingOrCurrent(currentAdditionalTime.getExtraFirstHalf(), newAdditionalTime.getExtraFirstHalf()))
                .extraSecondHalf(resolveIncomingOrCurrent(currentAdditionalTime.getExtraSecondHalf(), newAdditionalTime.getExtraSecondHalf()))
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

        MatchStatusDisplayResponse matchStatusDisplayResponse = matchStatusDisplayResolver.resolve(match, matchDetailDocument);

        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, matchStatusDisplayResponse.getDisplayText());
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

    private <T> T resolveIncomingOrCurrent(T currentValue, T incomingValue) {
        return incomingValue != null ? incomingValue : currentValue;
    }
}
