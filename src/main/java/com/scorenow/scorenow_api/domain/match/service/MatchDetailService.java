package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.StadiumData;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.ViewResult;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {

    private final StadiumCacheService stadiumCacheService;
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    @Transactional
    public void updateInplayMatchDetail(ApiProvider provider, ViewResult matchDetail) {
        String apiMatchId = matchDetail.getId();

        Match match = matchRepository.findByExternalInfo(provider, apiMatchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchStatus matchStatus = MatchStatus.fromCode(matchDetail.getTimeStatus());
        if (matchStatus != NOT_STARTED) {
            match.updateStatus(matchStatus);
        }
        match.updateHomeScore(matchDetail.getHomeScore());
        match.updateAwayScore(matchDetail.getAwayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        StadiumData stadiumData = matchDetail.getExtra().getStadiumData();
        if (stadiumData != null) {
            Stadium stadium = stadiumCacheService.getOrCreateStadium(provider, matchDetail.getSportId(), stadiumData.getId(), stadiumData.getName(), stadiumData.getCity());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (match.isStadiumEmpty()) {
                match.updateStadiumId(stadium.getId());
            }
        }

        // 경기 정보 반영
        MatchDetailDocument detail = matchDetail.toDocument(match.getId());
        matchDetailRepository.save(detail);

        log.info("✅ 경기 상세 데이터(MySQL & MongoDB) 동기화 완료 matchId:{}, apiMatchId:{}", match.getId(), apiMatchId);
    }

    /**
     * 어드민 수동 수정 (MySQL 스코어 + MongoDB 상세 데이터)
     */
    @Transactional
    public Long updateMatchDetailManual(Long matchId, MatchDetailUpdateRequest request) {
        // 스코어 및 경기 상태 수정
        matchRepository.findById(matchId).ifPresent(match -> {
            if (request.getStatus() != null) {
                try {
                    match.updateStatus(MatchStatus.valueOf(request.getStatus()));
                } catch (IllegalArgumentException e) {
                    log.error("잘못된 상태값입니다: {}", request.getStatus().replace('\n', '_').replace('\r', '_'));
                    throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
                }
            }

            // 홈-어웨이 스코어 변경
            if (request.getHomeScore() != null)
                match.updateHomeScore(request.getHomeScore());
            if (request.getAwayScore() != null)
                match.updateAwayScore(request.getAwayScore());
        });

        // 경기 기록 수정
        MatchDetailDocument detail = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        detail.updateFrom(request);

        log.info("📊 MatchDetailDocument 수동 수정 완료: {}", matchId);
        return matchDetailRepository.save(detail).getId();
    }

}