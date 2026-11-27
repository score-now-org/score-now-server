package com.scorenow.scorenow_api.domain.match.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.service.standings.TeamStandingLookupService;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppItemResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.EndedStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.service.appresponse.MatchAppSportResponseMapperRegistry;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.result.FootballMatchResultResolver;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.result.MatchResultResolverRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.InplayStatusDisplayResolverRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.odds.service.MatchOddsAppQueryService;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:bets-final-score;MODE=MySQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        EndedMatchSyncProcessor.class, MatchDetailService.class, MatchAppQueryService.class,
        MatchAppSportResponseMapperRegistry.class, MatchStatusDisplayResolver.class,
        InplayStatusDisplayResolverRegistry.class, MatchResultResolverRegistry.class, FootballMatchResultResolver.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BetsMatchFinalScoreIntegrationTest {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 11, 5, 18, 30);

    @Autowired private MatchAppQueryService appQueryService;
    @Autowired private EndedMatchSyncProcessor endedProcessor;
    @Autowired private MatchDetailService matchDetailService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;

    @MockBean private MatchDetailRepository detailRepository;
    @MockBean private MatchRealtimeEventPublisher eventPublisher;
    @MockBean private SportDetailEventProcessorRegistry sportEventProcessors;
    @MockBean private TeamExternalLookupService teamExternalLookupService;
    @MockBean private StadiumCacheService stadiumCacheService;
    @MockBean private StadiumRepository stadiumRepository;
    @MockBean private TeamStandingLookupService teamStandingLookupService;
    @MockBean private MatchOddsAppQueryService matchOddsAppQueryService;
    @MockBean private JPAQueryFactory queryFactory;
    @MockBean(name = "mongoMappingContext") private MongoMappingContext mongoMappingContext;

    @ParameterizedTest
    @CsvSource({"true, 1, 2", "false, 1, 2", "true, 3, 1", "false, 1, 3"})
    void View와_Ended의_저장_순서와_관계없이_앱은_Ended_최종_점수를_읽는다(
            boolean viewFirst, int viewHomeScore, int finalHomeScore) {
        Match match = insertMatch(MatchStatus.IN_PLAY);
        Long matchId = match.getId();
        givenFootballDetail(matchId, null);

        MatchDetailSyncSnapshot closingView = new MatchDetailSyncSnapshot(
                DataOrigin.BETS, SportCode.FOOTBALL, matchId, "1", null, null,
                viewHomeScore, 1, MatchStatus.ENDED, null, FootballDetail.empty());

        if (viewFirst) {
            matchDetailService.updateInplayMatchDetail(closingView);
            assertAppScore(match, viewHomeScore, 1);
            verify(eventPublisher).publishScoreChanged(matchId, viewHomeScore, 1);
            verify(eventPublisher).publishMatchStatusChanged(
                    matchId, MatchStatus.ENDED, viewHomeScore == 1 ? "무승부" : "홈팀 승");
            verifyNoMoreInteractions(eventPublisher);
            clearInvocations(eventPublisher);
        }

        assertThat(endedProcessor.process(leagueTarget(match), endedEvent(match, finalHomeScore + "-1"))).isTrue();
        int homeScoreBeforeEnded = viewFirst ? viewHomeScore : match.getHomeScore();
        if (finalHomeScore != homeScoreBeforeEnded) {
            verify(eventPublisher).publishScoreChanged(matchId, finalHomeScore, 1);
        }
        if (!viewFirst) {
            verify(eventPublisher).publishMatchStatusChanged(matchId, MatchStatus.ENDED, "종료");
            verifyNoMoreInteractions(eventPublisher);
            clearInvocations(eventPublisher, detailRepository, sportEventProcessors);
            matchDetailService.updateInplayMatchDetail(closingView);
            verifyNoInteractions(eventPublisher, detailRepository, sportEventProcessors);
        }

        assertAppScore(match, finalHomeScore, 1);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void Ended로_총점을_보정해도_저장된_승부차기로_승리팀을_표시한다() {
        Match match = insertMatch(MatchStatus.ENDED);
        FootballShootOutScore penalties = FootballShootOutScore.builder().homeScore(5).awayScore(4).build();
        AtomicReference<MatchDetailDocument> detail = givenFootballDetail(match.getId(), penalties);

        assertThat(endedProcessor.process(leagueTarget(match), endedEvent(match, "2-2"))).isTrue();

        MatchAppDetailResponse response = assertAppScore(match, 2, 2);
        EndedStatusDisplayDetailResponse result = (EndedStatusDisplayDetailResponse) response.statusDisplay().getDetail();
        assertThat(result.getResult()).isEqualTo("HOME_WIN");
        assertThat(result.getWinnerTeamId()).isEqualTo(match.getHomeId());
        assertThat(((FootballDetail) detail.get().getSportDetail()).getShootOutScore()).isSameAs(penalties);
        verify(eventPublisher).publishScoreChanged(match.getId(), 2, 2);
        verifyNoMoreInteractions(eventPublisher);
    }

    private Match insertMatch(MatchStatus status) {
        return new TransactionTemplate(transactionManager).execute(transaction -> {
            Sport sport = entityManager.createQuery(
                            "select s from Sport s where s.sportCode = :code", Sport.class)
                    .setParameter("code", SportCode.FOOTBALL)
                    .getResultList().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        Sport created = Sport.builder().sportCode(SportCode.FOOTBALL).build();
                        entityManager.persist(created);
                        return created;
                    });

            String externalId = UUID.randomUUID().toString();
            League league = League.builder().sportId(sport.getId()).eName(externalId).build();
            Team home = Team.builder().sport(sport).dataOrigin(DataOrigin.BETS).kName("홈팀").build();
            Team away = Team.builder().sport(sport).dataOrigin(DataOrigin.BETS).kName("원정팀").build();
            entityManager.persist(league);
            entityManager.persist(home);
            entityManager.persist(away);

            Match match = Match.builder()
                    .dataOrigin(DataOrigin.BETS)
                    .apiMatchId(externalId)
                    .sportId(sport.getId())
                    .leagueId(league.getId())
                    .homeId(home.getId())
                    .awayId(away.getId())
                    .statusCode(status)
                    .startAt(START_AT)
                    .homeScore(2)
                    .awayScore(1)
                    .build();
            entityManager.persist(match);
            return match;
        });
    }

    private AtomicReference<MatchDetailDocument> givenFootballDetail(Long matchId, FootballShootOutScore penalties) {
        AtomicReference<MatchDetailDocument> detail = new AtomicReference<>(MatchDetailDocument.builder()
                .id(matchId)
                .type(SportCode.FOOTBALL)
                .sportDetail(FootballDetail.builder().shootOutScore(penalties).build())
                .build());
        given(detailRepository.findById(matchId)).willAnswer(invocation -> Optional.of(detail.get()));
        given(detailRepository.findAllById(List.of(matchId))).willAnswer(invocation -> List.of(detail.get()));
        doAnswer(invocation -> {
            detail.set(invocation.getArgument(0));
            return null;
        }).when(detailRepository).upsertMatchDetail(any(MatchDetailDocument.class));
        return detail;
    }

    private MatchAppDetailResponse assertAppScore(Match match, int home, int away) {
        MatchAppDetailResponse detail = appQueryService.getMatch(match.getId());
        assertThat(detail.statusCode()).isEqualTo("ENDED");
        assertThat(detail.homeScore()).isEqualTo(home);
        assertThat(detail.awayScore()).isEqualTo(away);

        MatchAppItemResponse item = appQueryService
                .getMatchesWithFeatured(START_AT.toLocalDate(), SportCode.FOOTBALL, match.getLeagueId())
                .getLeagueMatches().get(0).getMatches().get(0);
        assertThat(item.getId()).isEqualTo(match.getId());
        assertThat(item.getStatusCode()).isEqualTo("ENDED");
        assertThat(item.getHomeScore()).isEqualTo(home);
        assertThat(item.getAwayScore()).isEqualTo(away);
        return detail;
    }

    private MatchSyncLeagueTarget leagueTarget(Match match) {
        return new MatchSyncLeagueTarget(DataOrigin.BETS, "1", "100",
                match.getSportId(), SportCode.FOOTBALL, match.getLeagueId(), "테스트 리그");
    }

    private BetsEventResponse.Event endedEvent(Match match, String score) {
        BetsEventResponse.League league = new BetsEventResponse.League();
        league.setId("100");
        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId(match.getApiMatchId());
        event.setLeague(league);
        event.setTimeStatus(MatchStatus.ENDED.getCode());
        event.setSs(score);
        return event;
    }
}
