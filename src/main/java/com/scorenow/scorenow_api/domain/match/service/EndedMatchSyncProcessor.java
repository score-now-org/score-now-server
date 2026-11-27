package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsScorePair;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EndedMatchSyncProcessor {

    private final MatchRepository matchRepository;
    private final MatchRealtimeEventPublisher eventPublisher;
    private final TeamExternalLookupService teamExternalLookupService;

    /**
     * Ended API로 기존 자동 관리 경기의 상태와 확인된 종료 총점을 보정한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(MatchSyncLeagueTarget leagueTarget, BetsEventResponse.Event event) {
        validateEvent(leagueTarget, event);

        if (event.getId() == null || event.getId().isBlank()) {
            return false;
        }

        Optional<Match> existingMatch = matchRepository.findForUpdateByDataOriginAndApiMatchId(
                leagueTarget.dataOrigin(), event.getId());

        if (existingMatch.isEmpty()) {
            log.warn(
                    "종료 상태 갱신 스킵 - 기존 경기 없음. dataOrigin:{}, apiMatchId:{}",
                    leagueTarget.dataOrigin(),
                    event.getId()
            );
            return false;
        }

        Match match = existingMatch.get();
        if (match.isManual()) {
            return false;
        }

        MatchStatus previousStatus = match.getStatusCode();
        MatchStatus newStatus = MatchStatus.fromCode(event.getTimeStatus());
        BetsScorePair finalScore = resolveFinalScore(leagueTarget, event, match);
        boolean statusChanged = previousStatus != newStatus;
        boolean scoreChanged = finalScore != null
                && (!Objects.equals(match.getHomeScore(), finalScore.home())
                || !Objects.equals(match.getAwayScore(), finalScore.away()));
        if (!statusChanged && !scoreChanged) {
            return false;
        }

        if (scoreChanged) {
            match.updateHomeScore(finalScore.home());
            match.updateAwayScore(finalScore.away());
        }
        if (statusChanged) {
            match.updateStatus(newStatus);
        }
        publishChangesAfterCommit(match.getId(), scoreChanged ? finalScore : null, statusChanged ? newStatus : null);

        log.info(
                "Ended API 경기 갱신. matchId:{}, apiMatchId:{}, previousStatus:{}, newStatus:{}, scoreChanged:{}",
                match.getId(),
                match.getApiMatchId(),
                previousStatus,
                newStatus,
                scoreChanged
        );
        return true;
    }

    private void publishChangesAfterCommit(Long matchId, BetsScorePair changedScore, MatchStatus changedStatus) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (changedScore != null) {
                    eventPublisher.publishScoreChanged(matchId, changedScore.home(), changedScore.away());
                }
                if (changedStatus != null) {
                    eventPublisher.publishMatchStatusChanged(matchId, changedStatus, changedStatus.getDescription());
                }
            }
        });
    }

    private BetsScorePair resolveFinalScore(MatchSyncLeagueTarget target, BetsEventResponse.Event event, Match match) {
        if (!MatchStatus.ENDED.getCode().equals(event.getTimeStatus())) {
            return null;
        }

        BetsScorePair score = BetsScorePair.parse(event.getSs());
        if (score == null || target.sportCode() != SportCode.BASEBALL) {
            return score;
        }

        String externalHomeId = event.getHome() == null ? null : event.getHome().getId();
        String externalAwayId = event.getAway() == null ? null : event.getAway().getId();
        if (externalHomeId == null || externalHomeId.isBlank()
                || externalAwayId == null || externalAwayId.isBlank()
                || externalHomeId.equals(externalAwayId)) {
            return null;
        }

        Map<String, Team> teams = teamExternalLookupService.findTeamsByExternalInfo(
                target.dataOrigin(), List.of(externalHomeId, externalAwayId));
        Team homeTeam = teams.get(externalHomeId);
        Team awayTeam = teams.get(externalAwayId);
        if (homeTeam == null || awayTeam == null) {
            return null;
        }

        Long homeId = homeTeam.getId();
        Long awayId = awayTeam.getId();
        if (homeId == null || awayId == null || homeId.equals(awayId)) {
            return null;
        }
        if (homeId.equals(match.getHomeId()) && awayId.equals(match.getAwayId())) {
            return score;
        }
        if (homeId.equals(match.getAwayId()) && awayId.equals(match.getHomeId())) {
            return new BetsScorePair(score.away(), score.home());
        }
        return null;
    }

    private void validateEvent(MatchSyncLeagueTarget leagueTarget, BetsEventResponse.Event event) {
        if (event.getLeague() == null || !leagueTarget.apiLeagueId().equals(event.getLeague().getId())) {
            String responseApiLeagueId = event.getLeague() == null ? null : event.getLeague().getId();

            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "리그 정보가 불일치 합니다. target_api_league_id: %s, response_api_league_id: %s"
                            .formatted(leagueTarget.apiLeagueId(), responseApiLeagueId)
            );
        }
    }
}
