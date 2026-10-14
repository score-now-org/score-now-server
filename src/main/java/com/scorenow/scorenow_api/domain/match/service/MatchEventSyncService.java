package com.scorenow.scorenow_api.domain.match.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.scorenow.scorenow_api.domain.league.service.LeagueService;
import com.scorenow.scorenow_api.domain.match.constant.MatchConstants;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.team.service.TeamService;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.external.betsapi.mapper.BetsEventSideMapper;
import com.scorenow.scorenow_api.external.betsapi.mapper.BetsEventSideMapper.MappedSides;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchEventSyncService {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(MatchConstants.SEOUL_TIME_ZONE);

    private final LeagueService leagueService;
    private final TeamService teamService;
    private final AdminFeaturedMatchService adminFeaturedMatchService;
    private final MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;
    private final BetsEventSideMapper betsEventSideMapper;

    private final MatchRepository matchRepository;
    private final MatchDetailInitializationService matchDetailInitializationService;

    /**
     * 개별 경기 동기화 - league → team → match 순서 보장
     * REQUIRES_NEW: 경기 1건 실패가 다른 경기에 영향 없음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncEvent(
            MatchSyncLeagueTarget leagueTarget,
            BetsEventResponse.Event event
    ) {
        // API 응답에 리그 정보가 없거나, 요청보낸 리그와 응답받은 리그 정보가 불일치 하는 경우
        if (event.getLeague() == null || !leagueTarget.apiLeagueId().equals(event.getLeague().getId())) {
            String targetApiLeagueId = leagueTarget.apiLeagueId();
            String responseApiLeagueId = event.getLeague() == null ? null : event.getLeague().getId();

            String message = "리그 정보가 불일치 합니다. target_api_league_id: %s, response_api_league_id: %s".formatted(targetApiLeagueId, responseApiLeagueId);
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, message);
        }

        DataOrigin dataOrigin = leagueTarget.dataOrigin();
        Long internalSportId = leagueTarget.internalSportId();
        League league = leagueService.getLeague(leagueTarget.internalLeagueId());
        MappedSides mappedSides = betsEventSideMapper.map(leagueTarget.sportCode(), event);

        Team homeTeam = saveTeam(dataOrigin, mappedSides.homeTeam(), internalSportId);
        Team awayTeam = saveTeam(dataOrigin, mappedSides.awayTeam(), internalSportId);

        saveMatch(
                event,
                leagueTarget,
                league,
                homeTeam,
                awayTeam,
                mappedSides.homeScore(),
                mappedSides.awayScore()
        );
    }

    private Team saveTeam(DataOrigin dataOrigin, BetsEventResponse.Team betsTeam, Long internalSportId) {
        if (betsTeam == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, String.format("%s 에서 팀 정보를 제공하지 않았습니다.", dataOrigin));
        }

        String imageUrl = teamService.buildImageUrl(betsTeam.getImageId());

        return teamService.getOrCreateTeam(dataOrigin, internalSportId, betsTeam.getId(), betsTeam.getName(), betsTeam.getCc(), imageUrl);
    }

    private void saveMatch(
            BetsEventResponse.Event event,
            MatchSyncLeagueTarget leagueTarget,
            League league,
            Team homeTeam,
            Team awayTeam,
            Integer homeScore,
            Integer awayScore) {

        if (event.getLeague() == null || event.getHome() == null || event.getAway() == null) {
            log.warn("경기 저장 스킵 - 필수 정보 누락 (league/home/away) eventId: {}", event.getId());
            return;
        }

        DataOrigin dataOrigin = leagueTarget.dataOrigin();
        Long internalSportId = leagueTarget.internalSportId();

        Match match = matchRepository.findByExternalInfo(dataOrigin, event.getId())
                .orElseGet(() -> Match.builder()
                        .dataOrigin(dataOrigin)
                        .apiMatchId(event.getId())
                        .bet365Id(event.getBet365Id())
                        .teamDisplayOrder(matchTeamDisplayOrderPolicy.decide(league))
                        .build());

        match.updateSportId(internalSportId);
        match.updateLeagueId(league.getId());
        match.updateHomeId(homeTeam.getId());
        match.updateAwayId(awayTeam.getId());
        match.updateStatus(MatchStatus.fromCode(event.getTimeStatus()));

        if (event.getTime() != null) {
            try {
                long timestamp = Long.parseLong(event.getTime());

                LocalDateTime newStartAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), DEFAULT_ZONE_ID);

                // 일자 기준 변경이 발생했을 때, 상단고정/핫매치 설정 해제 처리
                if (match.getId() != null && match.isStartDateChanged(newStartAt)) {
                    adminFeaturedMatchService.deleteFeaturedMatchByMatchId(match.getId());
                }

                match.updateStartAt(newStartAt);
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp format for eventId {}: time='{}'", event.getId(), event.getTime());
            }
        }

        if (homeScore != null && awayScore != null) {
            match.updateHomeScore(homeScore);
            match.updateAwayScore(awayScore);
        }

        Match savedMatch = matchRepository.save(match);

        // 없으면 초기 MatchDetailDocument 생성
        matchDetailInitializationService.createIfAbsent(savedMatch.getId(), leagueTarget.sportCode());

        log.debug("경기 저장 - {}", savedMatch.getId());
    }
}
