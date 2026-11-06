package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EndedMatchSyncProcessor {

    private final MatchRepository matchRepository;
    private final MatchRealtimeEventPublisher eventPublisher;

    /**
     * Ended API는 기존 경기의 최종 상태만 보정한다.
     * 응답에는 종료, 연기, 취소 등 실제 경기 종료로 분류된 상태만 포함된다고 간주한다.
     * 팀, 점수, 시작 시각, 상세 정보는 View API에서 관리하므로 변경하지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(MatchSyncLeagueTarget leagueTarget, BetsEventResponse.Event event) {
        validateEvent(leagueTarget, event);

        Optional<Match> existingMatch = matchRepository.findByExternalInfo(leagueTarget.dataOrigin(), event.getId());

        if (existingMatch.isEmpty()) {
            log.warn(
                    "종료 상태 갱신 스킵 - 기존 경기 없음. dataOrigin:{}, apiMatchId:{}",
                    leagueTarget.dataOrigin(),
                    event.getId()
            );
            return false;
        }

        Match match = existingMatch.get();
        MatchStatus previousStatus = match.getStatusCode();
        MatchStatus newStatus = MatchStatus.fromCode(event.getTimeStatus());
        if (previousStatus == newStatus) {
            return false;
        }

        match.updateStatus(newStatus);
        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, newStatus.getDescription());

        log.info(
                "Ended API 상태 갱신 완료. matchId:{}, apiMatchId:{}, previousStatus:{}, newStatus:{}",
                match.getId(),
                match.getApiMatchId(),
                previousStatus,
                newStatus
        );
        return true;
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
