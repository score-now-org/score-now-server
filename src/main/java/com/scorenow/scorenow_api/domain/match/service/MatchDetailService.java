package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.util.IdParser;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumService;
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
    private final StadiumService stadiumService;
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final BetsApiClient betsApiClient;

    @Transactional
    public void updateInplayMatchDetail(String sportId, String matchId) {
        String externalEventId = IdParser.extractEventId(matchId, sportId);
        BetsViewResponse response = betsApiClient.getEventView(externalEventId);

        if (response == null || !response.hasResult()) {
            log.warn("❌ 실패: ID {}에 대한 API 응답 데이터가 없습니다.", externalEventId);
            return;
        }

        // TODO: 서로 다른 DB 를 사용하기 때문에 한쪽에서 문제가 발생했을 때 롤백 정책을 어떻게 가져갈지 고민해야 할듯.
        BetsViewResponse.ViewResult apiResult = response.getResults().get(0);

        // 홈-어웨이 스코어 변경
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        updateMatchScore(match, apiResult.getHomeScore(), apiResult.getAwayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        BetsViewResponse.StadiumData stadiumData = apiResult.getExtra().getStadiumData();
        Stadium stadium = stadiumService.getOrCreateStadium(Stadium.of(stadiumData.getId(), stadiumData.getName(), apiResult.getSportId(), stadiumData.getCity()));
        match.updateStadiumId(stadium.getId());

        // 경기 정보 반영
        MatchDetailDocument detail = response.toDocument(matchId);
        matchDetailRepository.save(detail);

        log.info("✅ 성공: {} 경기 상세 데이터(MySQL & MongoDB) 동기화 완료", matchId);
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
                    log.error("잘못된 상태값입니다: {}", request.getStatus().replace('\n', '_').replace('\r', '_'));
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

    private void updateMatchScore(Match match, Integer homeScore, Integer awayScore) {
        match.updateHomeScore(homeScore);
        match.updateAwayScore(awayScore);
    }

}