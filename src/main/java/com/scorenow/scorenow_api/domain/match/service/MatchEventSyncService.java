package com.scorenow.scorenow_api.domain.match.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.scorenow.scorenow_api.domain.league.service.LeagueService;
import com.scorenow.scorenow_api.domain.match.constant.MatchConstants;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.service.TeamService;
import com.scorenow.scorenow_api.external.common.ApiProvider;
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
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchEventSyncService {

    private static final String TEAM_IMAGE_BASE_URL = "https://assets.b365api.com/images/team/m/";
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(MatchConstants.SEOUL_TIME_ZONE);

    private final LeagueService leagueService;
    private final TeamService teamService;
    private final MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;

    private final MatchRepository matchRepository;

    private final SportExternalMappingRepository sportExternalMappingRepository;

    /**
     * 개별 경기 동기화 - league → team → match 순서 보장
     * REQUIRES_NEW: 경기 1건 실패가 다른 경기에 영향 없음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncEvent(ApiProvider provider, BetsEventResponse.Event event, String externalSportId) {
        Long internalSportId = sportExternalMappingRepository.findByProviderAndApiSportId(provider, externalSportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND))
                .getInternalSportId();

        League savedLeague = saveLeague(provider, event.getLeague(), internalSportId);
        Team savedHome = saveTeam(provider, event.getHome(), internalSportId);
        Team savedAway = saveTeam(provider, event.getAway(), internalSportId);
        saveMatch(provider, event, internalSportId, savedLeague, savedHome, savedAway);
    }

    private League saveLeague(ApiProvider provider, BetsEventResponse.League betsLeague, Long internalSportId) {
        if (betsLeague == null) {
            throw new BusinessException(
                    ErrorCode.LEAGUE_NOT_FOUND,
                    String.format("%s 에서 리그 정보를 제공하지 않았습니다.", provider));
        }

        return leagueService.getOrCreateLeague(provider, internalSportId, betsLeague.getId(), betsLeague.getName(), betsLeague.getCc());
    }

    private Team saveTeam(ApiProvider provider, BetsEventResponse.Team betsTeam, Long internalSportId) {
        if (betsTeam == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, String.format("%s 에서 팀 정보를 제공하지 않았습니다.", provider));
        }

        String imageUrl = null;
        if (betsTeam.getImageId() != null) {
            imageUrl = TEAM_IMAGE_BASE_URL + betsTeam.getImageId() + ".png";
        }

        return teamService.getOrCreateTeam(provider, internalSportId, betsTeam.getId(), betsTeam.getName(), betsTeam.getCc(), imageUrl);
    }

    private void saveMatch(
            ApiProvider provider,
            BetsEventResponse.Event event,
            Long internalSportId,
            League league,
            Team homeTeam,
            Team awayTeam) {

        if (event.getLeague() == null || event.getHome() == null || event.getAway() == null) {
            log.warn("경기 저장 스킵 - 필수 정보 누락 (league/home/away) eventId: {}", event.getId());
            return;
        }

        Match match = matchRepository.findByExternalInfo(provider, event.getId())
                .orElseGet(() -> Match.builder()
                        .provider(provider)
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
                match.updateStartAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), DEFAULT_ZONE_ID));
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp format for eventId {}: time='{}'", event.getId(), event.getTime());
            }
        }

        if (event.getSs() != null && event.getSs().contains("-")) {
            String[] scores = event.getSs().split("-");
            if (scores.length == 2) {
                try {
                    match.updateHomeScore(Integer.parseInt(scores[0].trim()));
                    match.updateAwayScore(Integer.parseInt(scores[1].trim()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid score format for eventId {}: ss='{}'", event.getId(), event.getSs());
                }

            }
        }

        Match savedMatch = matchRepository.save(match);
        log.debug("경기 저장 - {}", savedMatch.getId());
    }
}
