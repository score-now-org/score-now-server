package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncStadium;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {
    private final StadiumCacheService stadiumCacheService;

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    private final MatchRealtimeEventPublisher eventPublisher;

    private final SportDetailEventProcessorRegistry eventProcessorRegistry;

    private final MatchStatusDisplayResolver matchStatusDisplayResolver;
    private final TeamExternalLookupService teamExternalLookupService;

    @Transactional
    public void updateInplayMatchDetail(MatchDetailSyncSnapshot snapshot) {
        DataOrigin dataOrigin = snapshot.dataOrigin();
        SportCode sportCode = snapshot.sportCode();
        Long matchId = snapshot.matchId();

        Match inplayMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        validateAndCorrectBaseballSides(inplayMatch, snapshot);

        MatchDetailDocument currentMatchDetailDocument = matchDetailRepository.findById(inplayMatch.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));

        if (sportCode != currentMatchDetailDocument.getType()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "경기 상세 종목 정보가 일치하지 않습니다.");
        }

        // 기존 종목 상세 정보와 정규화한 상세 정보를 비교하여 병합
        SportDetail mergedSportDetail = mergeSportDetail(
                sportCode,
                currentMatchDetailDocument.getSportDetail(),
                snapshot.sportDetail()
        );

        // 새로운 문서 생성
        MatchDetailDocument newMatchDetailDocument = MatchDetailDocument.builder()
                .id(inplayMatch.getId())
                .type(sportCode)
                .sportDetail(mergedSportDetail)
                .build();

        // 종목 공통 : 점수 업데이트 및 이벤트 발행
        updateMatchScoreAndPublishEvent(inplayMatch, snapshot.homeScore(), snapshot.awayScore());

        // 경기장 정보 생성 및 변경 (로컬 캐시 활용 + 더티체킹)
        MatchDetailSyncStadium stadium = snapshot.stadium();
        if (stadium != null) {
            Long stadiumId = stadiumCacheService.getOrCreateStadiumId(
                    dataOrigin,
                    snapshot.externalSportId(),
                    stadium.externalId(),
                    stadium.name(),
                    stadium.city());

            // 경기 정보에 경기장 정보가 할당되어 있지 않은 경우에만 할당
            if (inplayMatch.isStadiumEmpty()) {
                inplayMatch.updateStadiumId(stadiumId);
            }
        }

        // 종목 상세 : 종목별 상세 정보 업데이트 및 이벤트 발행
        eventProcessorRegistry.process(currentMatchDetailDocument, newMatchDetailDocument);

        // 경기 상세 정보 upsert
        matchDetailRepository.upsertMatchDetail(newMatchDetailDocument);

        // 종목 공통 : 경기 상태 업데이트 및 이벤트 발행
        updateMatchStatusAndPublishEvent(
                inplayMatch,
                newMatchDetailDocument,
                snapshot.status()
        );

        log.info("✅ 경기 상세 데이터(MySQL & MongoDB) 동기화 완료. dataOrigin:{} sportCode:{} matchId:{}, apiMatchId:{}",
                dataOrigin,
                sportCode,
                inplayMatch.getId(),
                inplayMatch.getApiMatchId()
        );
    }

    private SportDetail mergeSportDetail(
            SportCode sportCode,
            SportDetail currentSportDetail,
            SportDetail newSportDetail) {

        return switch (sportCode) {
            case FOOTBALL -> mergeFootballDetail(currentSportDetail, newSportDetail);
            case BASEBALL -> mergeBaseballDetail(currentSportDetail, newSportDetail);
            default -> newSportDetail;
        };
    }

    /**
     * 기존 야구 경기 상세 문서와 Bets View API 를 통해 얻은 야구 상세 문서를 병합한다.
     * 단, Bets 에서 제공하는 항목에 대해서만 교체를 진행한다.
     */
    private BaseballDetail mergeBaseballDetail(SportDetail currentSportDetail, SportDetail newSportDetail) {
        BaseballDetail currentBaseballDetail = resolveBaseballDetail(currentSportDetail);
        BaseballDetail newBaseballDetail = resolveBaseballDetail(newSportDetail);

        BaseballInningScores mergedInningScores = mergeBaseballInningScores(
                currentBaseballDetail.getInningScores(),
                newBaseballDetail.getInningScores()
        );
        BaseballHalfInning mergedCurrentHalfInning = mergedInningScores == null
                ? currentBaseballDetail.getCurrentHalfInning()
                : mergedInningScores.resolveCurrentHalfInning();

        BaseballDetail merged = currentBaseballDetail;
        if (mergedInningScores != null) {
            merged = merged.withInningScores(mergedInningScores);
        }

        if (mergedCurrentHalfInning != null) {
            merged = merged.withCurrentHalfInning(mergedCurrentHalfInning);
        }

        return merged;
    }

    private BaseballInningScores mergeBaseballInningScores(BaseballInningScores current, BaseballInningScores incoming) {
        if (current == null) {
            return incoming;
        }

        return current.mergeProvided(incoming);
    }

    private void validateAndCorrectBaseballSides(Match match, MatchDetailSyncSnapshot snapshot) {
        if (snapshot.sportCode() != SportCode.BASEBALL) {
            return;
        }

        String externalHomeTeamId = snapshot.externalHomeTeamId();
        String externalAwayTeamId = snapshot.externalAwayTeamId();
        if (externalHomeTeamId == null || externalHomeTeamId.isBlank()
                || externalAwayTeamId == null || externalAwayTeamId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "야구 View API의 홈·원정 팀 정보가 누락되었습니다."
            );
        }

        Map<String, Team> teamsByExternalId = teamExternalLookupService.findTeamsByExternalInfo(
                snapshot.dataOrigin(),
                List.of(externalHomeTeamId, externalAwayTeamId)
        );
        Team viewHomeTeam = teamsByExternalId.get(externalHomeTeamId);
        Team viewAwayTeam = teamsByExternalId.get(externalAwayTeamId);
        if (viewHomeTeam == null || viewAwayTeam == null) {
            throw new BusinessException(
                    ErrorCode.TEAM_NOT_FOUND,
                    "야구 View API의 팀 외부 매핑을 찾을 수 없습니다."
            );
        }

        Long viewHomeId = viewHomeTeam.getId();
        Long viewAwayId = viewAwayTeam.getId();

        // 기존 Match 에 저장되어 있는 홈-원정 팀 ID 와 View API 로 들어온 홈-원정 팀 ID가 동일하다면 검증 종료
        if (Objects.equals(match.getHomeId(), viewHomeId) && Objects.equals(match.getAwayId(), viewAwayId)) {
            return;
        }

        // 기존 Match 에 저장되어 있는 홈-원정 팀 ID 와 View API 로 들어온 홈-원정 팀 ID가 거꾸로 매핑되어 있다면 보정
        if (Objects.equals(match.getHomeId(), viewAwayId) && Objects.equals(match.getAwayId(), viewHomeId)) {
            Long previousHomeId = match.getHomeId();
            Long previousAwayId = match.getAwayId();
            match.updateHomeId(viewHomeId);
            match.updateAwayId(viewAwayId);

            log.warn(
                    "🟠야구 경기 홈·원정 자동 교정. matchId:{}, apiMatchId:{}, before:[home={}, away={}], after:[home={}, away={}]",
                    match.getId(),
                    match.getApiMatchId(),
                    previousHomeId,
                    previousAwayId,
                    viewHomeId,
                    viewAwayId
            );
            return;
        }

        throw new BusinessException(ErrorCode.INVALID_PARAMETER, "야구 View API 경기 팀 정보가 기존 Match와 일치하지 않습니다.");
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

    private BaseballDetail resolveBaseballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return BaseballDetail.empty();
        }

        if (!(sportDetail instanceof BaseballDetail baseballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "야구 상세 정보가 아닙니다.");
        }

        return baseballDetail;
    }

    private <T> T resolveIncomingOrCurrent(T currentValue, T incomingValue) {
        return incomingValue != null ? incomingValue : currentValue;
    }
}
