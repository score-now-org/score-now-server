package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.service.LeagueService;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchEventSyncServiceTest {

    @InjectMocks
    private MatchEventSyncService matchEventSyncService;

    @Mock
    private LeagueService leagueService;
    @Mock
    private TeamService teamService;
    @Mock
    private AdminFeaturedMatchService adminFeaturedMatchService;
    @Mock
    private MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailRepository matchDetailRepository;
    @Mock
    private SportExternalMappingRepository sportExternalMappingRepository;

    @Test
    void 자동_동기화_경기_저장시_초기_MatchDetailDocument를_생성한다() {
        BetsEventResponse.Event event = createEvent();
        League league = League.builder()
                .id(2L)
                .sportId(1L)
                .build();
        Team homeTeam = Team.builder().id(10L).build();
        Team awayTeam = Team.builder().id(20L).build();

        given(sportExternalMappingRepository.findByProviderAndApiSportId(DataOrigin.BETS, "1"))
                .willReturn(Optional.of(SportExternalMapping.of(DataOrigin.BETS, "1", 1L)));
        given(leagueService.getOrCreateLeague(DataOrigin.BETS, 1L, "100", "Premier League", "GB"))
                .willReturn(league);
        given(teamService.buildImageUrl("home-image")).willReturn("home-image-url");
        given(teamService.buildImageUrl("away-image")).willReturn("away-image-url");
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "10", "Home", "KR", "home-image-url"))
                .willReturn(homeTeam);
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "20", "Away", "JP", "away-image-url"))
                .willReturn(awayTeam);
        given(matchTeamDisplayOrderPolicy.decide(league)).willReturn(TeamDisplayOrder.HOME_AWAY);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId())).willReturn(Optional.empty());
        given(matchRepository.save(any(Match.class))).willAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            ReflectionTestUtils.setField(match, "id", 30L);
            return match;
        });

        matchEventSyncService.syncEvent(DataOrigin.BETS, event, "1");

        then(matchDetailRepository).should()
                .createInitialMatchDetailIfAbsent(30L, SportDetailType.FOOTBALL);
    }

    private BetsEventResponse.Event createEvent() {
        BetsEventResponse.League league = new BetsEventResponse.League();
        league.setId("100");
        league.setName("Premier League");
        league.setCc("GB");

        BetsEventResponse.Team home = new BetsEventResponse.Team();
        home.setId("10");
        home.setName("Home");
        home.setImageId("home-image");
        home.setCc("KR");

        BetsEventResponse.Team away = new BetsEventResponse.Team();
        away.setId("20");
        away.setName("Away");
        away.setImageId("away-image");
        away.setCc("JP");

        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId("api-1000");
        event.setTimeStatus("0");
        event.setLeague(league);
        event.setHome(home);
        event.setAway(away);
        return event;
    }
}
