package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchScheduledStartAtUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchStatusUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAdminMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.BaseballMatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.service.SportCodeResolver;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BaseballAdminMatchDetailService {

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final BaseballMatchDetailRepository baseballMatchDetailRepository;

    private final MatchRealtimeEventPublisher eventPublisher;
    private final SportDetailEventProcessorRegistry eventProcessorRegistry;

    private final MatchStatusDisplayResolver matchStatusDisplayResolver;
    private final MatchScheduledStartAtUpdater matchScheduledStartAtUpdater;

    private final SportCodeResolver sportCodeResolver;

    /**
     * 야구 경기 중계 데이터 조회
     */
    @Transactional(readOnly = true)
    public BaseballAdminMatchDetailResponse getBaseballMatchDetail(Long matchId) {
        Match match = findBaseballMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        // 상세 중계 설정 여부에 따른 응답값 생성
        BaseballAdminMatchDetailResponse.BaseballDetailResponse detailResponse =
                baseballDetail.isDetailedBroadcastEnabled()
                        ? BaseballAdminMatchDetailResponse.BaseballDetailResponse.fromDetailed(baseballDetail)
                        : BaseballAdminMatchDetailResponse.BaseballDetailResponse.fromBasic(baseballDetail);

        return BaseballAdminMatchDetailResponse.builder()
                .startAt(match.getStartAt())
                .statusCode(match.getStatusCode().name())
                .statusName(match.getStatusCode().getDescription())
                .homeScore(match.getHomeScore())
                .awayScore(match.getAwayScore())
                .currentCommentary(matchDetailDocument.getCurrentCommentary())
                .currentCommentaryHighlighted(matchDetailDocument.isCurrentCommentaryHighlighted())
                .baseballDetail(detailResponse)
                .build();
    }

    /**
     * 경기 예정 시각 수정
     */
    @Transactional
    public void updateMatchScheduledStartAt(Long matchId, MatchScheduledStartAtUpdateRequest request) {
        Match match = findBaseballMatchByMatchId(matchId);
        matchScheduledStartAtUpdater.update(match, request.getStartAt());
    }

    /**
     * 경기 상태 수정
     */
    @Transactional
    public void updateMatchStatus(Long matchId, MatchStatusUpdateRequest request) {
        Match match = findBaseballMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);

        updateMatchStatusAndPublishEvent(match, matchDetailDocument, request.getStatus());
    }

    /**
     * 현재 이닝 수정
     */
    public void updateCurrentHalfInning(Long matchId, BaseballHalfInning halfInning) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withCurrentHalfInning(halfInning);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updateCurrentHalfInning(
                matchId,
                newBaseballDetail.getCurrentHalfInning(),
                newBaseballDetail.getCount()
        );

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 이닝별 점수 수정
     */
    @Transactional
    public void updateInningScores(Long matchId, BaseballInningScores scores) {
        Match match = findBaseballMatchByMatchId(matchId);
        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        int totalHomeScore = scores.totalHomeScore();
        int totalAwayScore = scores.totalAwayScore();

        boolean scoreChanged =
                !Objects.equals(match.getHomeScore(), totalHomeScore)
                        || !Objects.equals(match.getAwayScore(), totalAwayScore);

        if (scoreChanged) {
            match.updateHomeScore(totalHomeScore);
            match.updateAwayScore(totalAwayScore);

            eventPublisher.publishScoreChanged(matchId, totalHomeScore, totalAwayScore);
        }

        // MatchDetailDocument.baseballDetail 의 야구 이닝별 점수 수정 (아예 교체하는 방식으로 하면 될 듯)
        BaseballDetail newBaseballDetail = baseballDetail.withInningScores(scores);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updateInningScores(matchId, newBaseballDetail.getInningScores());

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 스텟 수정 (H E B)
     */
    public void updateBaseballStats(Long matchId, BaseballStats homeStats, BaseballStats awayStats) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withStats(homeStats, awayStats);

        baseballMatchDetailRepository.updateStats(matchId, newBaseballDetail.getHomeStats(), newBaseballDetail.getAwayStats());
    }

    /**
     * 볼 / 아웃 카운트 수정
     */
    public void updateBaseballCount(Long matchId, BaseballCount count) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withCount(count);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updateCount(matchId, newBaseballDetail.getCount());

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 베이스별 정보 수정
     */
    public void updateBaseStates(Long matchId, BaseballBaseStates baseStates) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withBaseStates(baseStates);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updateBaseStates(matchId, newBaseballDetail.getBaseStates());

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 현재 투수 정보 수정
     * TODO: 추후 선수 데이터와 직접 연동 해야 함.
     */
    public void updatePitcher(Long matchId, String pitcher) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withPitcher(pitcher);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updatePitcher(matchId, newBaseballDetail.getPitcher());

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }

    /**
     * 현재 타자 정보 수정
     * TODO: 추후 선수 데이터와 직접 연동 해야 함.
     */
    public void updateBatter(Long matchId, String batter) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        BaseballDetail newBaseballDetail = baseballDetail.withBatter(batter);
        MatchDetailDocument newMatchDetailDocument = matchDetailDocument.withSportDetail(newBaseballDetail);

        baseballMatchDetailRepository.updateBatter(matchId, newBaseballDetail.getBatter());

        eventProcessorRegistry.process(matchDetailDocument, newMatchDetailDocument);
    }


    /**
     * 주자, 투수정보, 볼/아웃 카운트, HEB 정보 제공 ON OFF
     */
    public void updateDetailedBroadcastEnabled(Long matchId, boolean isEnabled) {
        findBaseballMatchByMatchId(matchId);

        MatchDetailDocument matchDetailDocument = findMatchDetailDocumentByMatchId(matchId);
        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument.getSportDetail());

        if (baseballDetail.isDetailedBroadcastEnabled() != isEnabled) {
            BaseballDetail newBaseballDetail = baseballDetail.withDetailedBroadcastEnabled(isEnabled);
            baseballMatchDetailRepository.updateDetailedBroadcastEnabled(matchId, newBaseballDetail.isDetailedBroadcastEnabled());
        }
    }

    private Match findBaseballMatchByMatchId(Long matchId) {
        Match match = findMatchByMatchId(matchId);

        SportCode sportCode = sportCodeResolver.resolveByInternalSportId(match.getSportId());

        if (sportCode != SportCode.BASEBALL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "야구 경기가 아닙니다.");
        }

        return match;
    }

    private Match findMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private MatchDetailDocument findMatchDetailDocumentByMatchId(Long matchId) {
        return matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));
    }

    private void updateMatchStatusAndPublishEvent(Match match, MatchDetailDocument matchDetailDocument, MatchStatus newStatus) {
        if (Objects.equals(match.getStatusCode(), newStatus)) {
            return;
        }

        match.updateStatus(newStatus);

        MatchStatusDisplayResponse matchStatusDisplayResponse = matchStatusDisplayResolver.resolve(match, matchDetailDocument);
        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, matchStatusDisplayResponse.getDisplayText());
    }

    private BaseballDetail resolveBaseballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return BaseballDetail.empty();
        }

        if (!(sportDetail instanceof BaseballDetail baseballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "야구 상세 정보가 아닙니다.");
        }

        return baseballDetail;
    }
}
