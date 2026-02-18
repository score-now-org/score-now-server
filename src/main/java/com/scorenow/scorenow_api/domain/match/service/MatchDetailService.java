package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final BetsApiClient betsApiClient;

    @Transactional
    public void updateInplayMatchDetail(String eventId) {
        String pureId = eventId.replaceAll("[^0-9]", "");
        BetsViewResponse response = betsApiClient.getEventView(pureId);

        if (response == null || !response.hasResult()) {
            log.warn("❌ 실패: ID {}에 대한 API 응답 데이터가 없습니다.", pureId);
            return;
        }

        BetsViewResponse.ViewResult apiResult = response.getResults().get(0);
        updateMySqlScore(eventId, apiResult);

        MatchDetailDocument detail = response.toDocument(eventId);
        matchDetailRepository.save(detail);

        log.info("✅ 성공: {} 경기 상세 데이터(MySQL & MongoDB) 동기화 완료", eventId);
    }

    private void updateMySqlScore(String eventId, BetsViewResponse.ViewResult result) {
        matchRepository.findById(eventId).ifPresent(match -> {
            match.updateHomeScore(result.getHomeScore());
            match.updateAwayScore(result.getAwayScore());
        });
    }

    /**
     * 어드민 수동 수정 (MySQL 스코어 + MongoDB 상세 데이터)
     */
    @Transactional
    public String updateMatchDetailManual(String eventId, MatchDetailUpdateRequest request) {
        // 스코어 및 경기 상태 수정
        matchRepository.findById(eventId).ifPresent(match -> {
            if (request.getStatus() != null) {
                try {
                    match.updateStatus(MatchStatus.valueOf(request.getStatus()));
                } catch (IllegalArgumentException e) {
                    log.error("잘못된 상태값입니다: {}", request.getStatus());
                    throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
                }
            }

            if (request.getHomeScore() != null) match.updateHomeScore(request.getHomeScore());
            if (request.getAwayScore() != null) match.updateAwayScore(request.getAwayScore());
        });

        // 경기 기록 수정
        MatchDetailDocument detail = matchDetailRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        detail.updateFrom(request);

        log.info("📊 MongoDB 상세 지표 수동 수정 완료: {}", eventId);
        return matchDetailRepository.save(detail).getId();
    }
}