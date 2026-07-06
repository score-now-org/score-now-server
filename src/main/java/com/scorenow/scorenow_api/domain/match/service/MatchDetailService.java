package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
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

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.ENDED;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {

    private final StadiumCacheService stadiumCacheService;
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    private final MatchClockSyncService matchClockSyncService;
    private final MatchRealtimeEventPublisher eventPublisher;

    @Transactional
    public void updateInplayMatchDetail(DataOrigin dataOrigin, ViewResult viewResult) {
        String apiMatchId = viewResult.getId();

        Match match = matchRepository.findByExternalInfo(dataOrigin, apiMatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 점수에 변동 사항이 있는 경우 점수 업데이트 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, viewResult.getHomeScore(), viewResult.getAwayScore());

        // 경기 상태 업데이트 및 이벤트 발행
        updateMatchStatusAndPublishEvent(match, MatchStatus.fromCode(viewResult.getTimeStatus()));

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        StadiumData stadiumData = viewResult.getExtra().getStadiumData();
        if (stadiumData != null) {
            Stadium stadium = stadiumCacheService.getOrCreateStadium(dataOrigin, viewResult.getSportId(), stadiumData.getId(), stadiumData.getName(), stadiumData.getCity());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (match.isStadiumEmpty()) {
                match.updateStadiumId(stadium.getId());
            }
        }

        // 경기 상세 정보 반영
        MatchDetailDocument newMatchDetail = viewResult.toDocument(match.getId(), match.getStartAt());
        matchDetailRepository.upsertMatchDetail(newMatchDetail);

        // 경기 시간 정보 반영 (경기 시간 정보가 제공되는 경우에만 / 경기전,경기종료의 경우 시간 정보가 제공되지 않는다.)
        MatchDetailDocument.MatchClock newMatchClock = newMatchDetail.getMatchClock();
        if (newMatchClock != null) {
            matchClockSyncService.syncMatchClock(match.getId(), newMatchClock);
        }

        log.info("✅ 경기 상세 데이터(MySQL & MongoDB) 동기화 완료 matchId:{}, apiMatchId:{}", match.getId(), apiMatchId);
    }

    /**
     * 어드민 수동 수정 (MySQL 스코어 + MongoDB 상세 데이터)
     */
    @Transactional
    public Long updateMatchDetailManual(Long matchId, MatchDetailUpdateRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 점수에 변동 사항이 있는 경우 점수 업데이트 진행 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, request.getHomeScore(), request.getAwayScore());

        // 경기 상태 업데이트 및 이벤트 발행
        if (request.getStatus() != null) {
            updateMatchStatusAndPublishEvent(match, request.getStatus());
        }

        // 경기 상세 정보 반영
        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        matchDetailDocument.updateFrom(request);

        MatchDetailDocument savedMatchDetailDocument = matchDetailRepository.save(matchDetailDocument);

        // 경기 시간 정보 반영
        if (request.getMatchClock() != null) {
            matchClockSyncService.syncManualMatchClock(savedMatchDetailDocument.getId(), request.getMatchClock());
        }

        return savedMatchDetailDocument.getId();
    }

    /**
     * 점수에 변동 사항이 있는 경우 점수 업데이트 진행 및 이벤트 발행
     */
    private void updateMatchScoreAndPublishEvent(Match match, Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            return;
        }

        Integer beforeHomeScore = match.getHomeScore();
        Integer beforeAwayScore = match.getAwayScore();

        boolean scoreChanged = !homeScore.equals(beforeHomeScore) || !awayScore.equals(beforeAwayScore);

        if (scoreChanged) {
            match.updateHomeScore(homeScore);
            match.updateAwayScore(awayScore);

            eventPublisher.publishScoreChanged(match.getId(), homeScore, awayScore);
        }
    }

    /**
     * 경기 상태 업데이트 및 이벤트 발행
     */
    private void updateMatchStatusAndPublishEvent(Match match, MatchStatus newStatus) {
        MatchStatus currentStatus = match.getStatusCode();

        if (newStatus == NOT_STARTED) {
            return;
        }

        if (currentStatus == newStatus) {
            return;
        }

        match.updateStatus(newStatus);

        if (newStatus == ENDED) {
            eventPublisher.publishMatchStatusChanged(
                    match.getId(),
                    newStatus,
                    MatchResult.fromScore(match.getHomeScore(), match.getAwayScore()));

            return;
        }

        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus);
    }

}