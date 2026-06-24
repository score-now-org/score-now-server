package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
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

import java.util.Objects;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {

    private final StadiumCacheService stadiumCacheService;
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    private final MatchRealtimeEventPublisher eventPublisher;

    @Transactional
    public void updateInplayMatchDetail(DataOrigin provider, ViewResult viewResult) {
        String apiMatchId = viewResult.getId();

        Match match = matchRepository.findByExternalInfo(provider, apiMatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchStatus matchStatus = MatchStatus.fromCode(viewResult.getTimeStatus());
        if (matchStatus != NOT_STARTED) {
            match.updateStatus(matchStatus);
        }

        // 점수에 변동 사항이 있는 경우 점수 업데이트 진행 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, viewResult.getHomeScore(), viewResult.getAwayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        StadiumData stadiumData = viewResult.getExtra().getStadiumData();
        if (stadiumData != null) {
            Stadium stadium = stadiumCacheService.getOrCreateStadium(provider, viewResult.getSportId(), stadiumData.getId(), stadiumData.getName(), stadiumData.getCity());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (match.isStadiumEmpty()) {
                match.updateStadiumId(stadium.getId());
            }
        }

        // 경기 정보 반영
        MatchDetailDocument matchDetailDocument = viewResult.toDocument(match.getId(), match.getStartAt());
        matchDetailRepository.upsertMatchDetail(matchDetailDocument);

        log.info("✅ 경기 상세 데이터(MySQL & MongoDB) 동기화 완료 matchId:{}, apiMatchId:{}", match.getId(), apiMatchId);
    }

    /**
     * 어드민 수동 수정 (MySQL 스코어 + MongoDB 상세 데이터)
     */
    @Transactional
    public Long updateMatchDetailManual(Long matchId, MatchDetailUpdateRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 경기 상태 수정
        if (request.getStatus() != null) {
            match.updateStatus(MatchStatus.valueOf(request.getStatus()));
        }

        // 점수에 변동 사항이 있는 경우 점수 업데이트 진행 및 이벤트 발행
        updateMatchScoreAndPublishEvent(match, request.getHomeScore(), request.getAwayScore());

        // 경기 기록 수정
        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        matchDetailDocument.updateFrom(request);

        log.info("📊 MatchDetailDocument 수동 수정 완료: {}", matchId);
        return matchDetailRepository.save(matchDetailDocument).getId();
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

}