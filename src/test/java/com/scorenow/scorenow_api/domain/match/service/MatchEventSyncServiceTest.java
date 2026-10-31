package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.service.LeagueService;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.odds.service.MatchOddsDisplayPolicyInitializationService;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.external.betsapi.mapper.BetsEventSideMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
    @Spy
    private BetsEventSideMapper betsEventSideMapper;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailInitializationService matchDetailInitializationService;
    @Mock
    private MatchOddsDisplayPolicyInitializationService matchOddsDisplayPolicyInitializationService;

    @Test
    void 자동_동기화로_신규_경기_저장시_초기_데이터를_생성한다() {
        BetsEventResponse.Event event = createEvent();
        MatchSyncLeagueTarget leagueTarget = new MatchSyncLeagueTarget(
                DataOrigin.BETS,
                "1",
                "100",
                1L,
                SportCode.FOOTBALL,
                2L,
                "Premier League"
        );
        League league = League.builder()
                .id(2L)
                .sportId(1L)
                .build();
        Team homeTeam = Team.builder().id(10L).build();
        Team awayTeam = Team.builder().id(20L).build();

        given(leagueService.getLeague(2L)).willReturn(league);
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
        matchEventSyncService.syncEvent(leagueTarget, event);

        then(leagueService).should().getLeague(2L);
        then(matchDetailInitializationService).should()
                .createIfAbsent(30L, SportCode.FOOTBALL);
        then(matchOddsDisplayPolicyInitializationService).should()
                .initialize(30L);
    }

    @Test
    void 자동_동기화로_기존_경기_갱신시_배당률_노출_정책을_다시_초기화하지_않는다() {
        BetsEventResponse.Event event = createEvent();
        MatchSyncLeagueTarget leagueTarget = new MatchSyncLeagueTarget(
                DataOrigin.BETS,
                "1",
                "100",
                1L,
                SportCode.FOOTBALL,
                2L,
                "Premier League"
        );
        League league = League.builder()
                .id(2L)
                .sportId(1L)
                .build();
        Team homeTeam = Team.builder().id(10L).build();
        Team awayTeam = Team.builder().id(20L).build();
        Match existingMatch = Match.builder()
                .id(30L)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId(event.getId())
                .build();

        given(leagueService.getLeague(2L)).willReturn(league);
        given(teamService.buildImageUrl("home-image")).willReturn("home-image-url");
        given(teamService.buildImageUrl("away-image")).willReturn("away-image-url");
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "10", "Home", "KR", "home-image-url"))
                .willReturn(homeTeam);
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "20", "Away", "JP", "away-image-url"))
                .willReturn(awayTeam);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.of(existingMatch));
        given(matchRepository.save(existingMatch)).willReturn(existingMatch);

        matchEventSyncService.syncEvent(leagueTarget, event);

        then(matchOddsDisplayPolicyInitializationService).should(never())
                .initialize(anyLong());
    }

    @Test
    void 야구는_Bets의_팀과_점수_순서를_반대로_저장한다() {
        BetsEventResponse.Event event = createEvent();
        event.setSs("4-1");

        MatchSyncLeagueTarget leagueTarget = new MatchSyncLeagueTarget(
                DataOrigin.BETS,
                "16",
                "100",
                1L,
                SportCode.BASEBALL,
                2L,
                "KBO"
        );
        League league = League.builder()
                .id(2L)
                .sportId(1L)
                .build();
        Team actualHomeTeam = Team.builder().id(20L).build();
        Team actualAwayTeam = Team.builder().id(10L).build();

        given(leagueService.getLeague(2L)).willReturn(league);
        given(teamService.buildImageUrl("home-image")).willReturn("home-image-url");
        given(teamService.buildImageUrl("away-image")).willReturn("away-image-url");
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "20", "Away", "JP", "away-image-url"))
                .willReturn(actualHomeTeam);
        given(teamService.getOrCreateTeam(DataOrigin.BETS, 1L, "10", "Home", "KR", "home-image-url"))
                .willReturn(actualAwayTeam);
        given(matchTeamDisplayOrderPolicy.decide(league)).willReturn(TeamDisplayOrder.HOME_AWAY);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId())).willReturn(Optional.empty());
        given(matchRepository.save(any(Match.class))).willAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            ReflectionTestUtils.setField(match, "id", 30L);
            return match;
        });

        matchEventSyncService.syncEvent(leagueTarget, event);

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        then(matchRepository).should().save(matchCaptor.capture());
        Match savedMatch = matchCaptor.getValue();

        assertThat(savedMatch.getHomeId()).isEqualTo(20L);
        assertThat(savedMatch.getAwayId()).isEqualTo(10L);
        assertThat(savedMatch.getHomeScore()).isEqualTo(1);
        assertThat(savedMatch.getAwayScore()).isEqualTo(4);
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
