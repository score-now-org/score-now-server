package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class EndedMatchSyncProcessorTest {

    private static final Long MATCH_ID = 10L;
    private static final String EXTERNAL_MATCH_ID = "api-1000";

    @InjectMocks
    private EndedMatchSyncProcessor processor;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;
    @Mock
    private TeamExternalLookupService teamExternalLookupService;

    @BeforeEach
    void 트랜잭션_동기화를_시작한다() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void 트랜잭션_동기화를_정리한다() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void 진행중_경기의_종료_상태와_최종_점수를_저장한다() {
        Match match = givenMatch(MatchStatus.IN_PLAY);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "2-0"))).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, 2, 0);
    }

    @Test
    void 이미_종료된_경기도_유효한_점수로_보정한다() {
        Match match = givenMatch(MatchStatus.ENDED);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "0-0"))).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, 0, 0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2", "2-", "-2-0", "2-0-1", "2-오류", "2147483648-0"})
    void 불완전한_점수는_저장값을_유지하고_종료_상태를_반영한다(String ss) {
        Match match = givenMatch(MatchStatus.IN_PLAY);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, ss))).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, 1, 0);
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"POSTPONED", "CANCELLED", "ABANDONED"})
    void 취소나_연기는_상태만_변경한다(MatchStatus status) {
        Match match = givenMatch(MatchStatus.IN_PLAY);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(status, "2-1"))).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(status);
        assertScore(match, 1, 0);
    }

    @ParameterizedTest
    @CsvSource({"1, 2, 4, 1", "2, 1, 1, 4"})
    void 야구_점수를_외부_팀_ID에_대응하는_홈과_원정에_저장한다(
            long betsHomeId, long betsAwayId, int homeScore, int awayScore) {
        Match match = givenMatch(MatchStatus.IN_PLAY);
        BetsEventResponse.Event event = baseballEvent();
        given(teamExternalLookupService.findTeamsByExternalInfo(DataOrigin.BETS, List.of("A", "B")))
                .willReturn(Map.of("A", team(betsHomeId), "B", team(betsAwayId)));

        assertThat(processor.process(leagueTarget(SportCode.BASEBALL), event)).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, homeScore, awayScore);
        assertThat(match.getHomeId()).isEqualTo(1L);
        assertThat(match.getAwayId()).isEqualTo(2L);
    }

    @Test
    void 야구_외부_팀_ID가_누락되면_상태만_변경한다() {
        Match match = givenMatch(MatchStatus.IN_PLAY);
        BetsEventResponse.Event event = baseballEvent();
        event.setAway(null);

        assertThat(processor.process(leagueTarget(SportCode.BASEBALL), event)).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, 1, 0);
        then(teamExternalLookupService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "3"})
    void 야구_두_팀을_서로_다른_저장_팀에_대응시킬_수_없으면_상태만_변경한다(String awayId) {
        Match match = givenMatch(MatchStatus.IN_PLAY);
        Map<String, Team> teams = awayId == null || awayId.isEmpty()
                ? Map.of("A", team(1L))
                : Map.of("A", team(1L), "B", team(Long.parseLong(awayId)));
        given(teamExternalLookupService.findTeamsByExternalInfo(DataOrigin.BETS, List.of("A", "B")))
                .willReturn(teams);

        assertThat(processor.process(leagueTarget(SportCode.BASEBALL), baseballEvent())).isTrue();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertScore(match, 1, 0);
    }

    @Test
    void 수동_관리_경기는_상태와_점수를_유지한다() {
        Match match = givenMatch(MatchStatus.IN_PLAY);
        match.updateIsManual(true);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "2-1"))).isFalse();

        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.IN_PLAY);
        assertScore(match, 1, 0);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 동일한_상태와_점수는_갱신하지_않는다() {
        givenMatch(MatchStatus.ENDED);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "1-0"))).isFalse();

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 점수만_보정되면_커밋_이후_점수_이벤트만_한_번_발행한다() {
        givenMatch(MatchStatus.ENDED);
        MatchSyncLeagueTarget target = leagueTarget(SportCode.FOOTBALL);
        BetsEventResponse.Event event = event(MatchStatus.ENDED, "2-0");

        assertThat(processor.process(target, event)).isTrue();
        assertThat(processor.process(target, event)).isFalse();

        then(eventPublisher).shouldHaveNoInteractions();
        afterCommit();
        then(eventPublisher).should().publishScoreChanged(MATCH_ID, 2, 0);
        then(eventPublisher).shouldHaveNoMoreInteractions();
    }

    @Test
    void 점수와_상태가_모두_변경되면_커밋_이후_점수_이벤트를_먼저_발행한다() {
        givenMatch(MatchStatus.IN_PLAY);
        MatchSyncLeagueTarget target = leagueTarget(SportCode.FOOTBALL);
        BetsEventResponse.Event event = event(MatchStatus.ENDED, "2-0");

        assertThat(processor.process(target, event)).isTrue();

        then(eventPublisher).shouldHaveNoInteractions();
        afterCommit();
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishScoreChanged(MATCH_ID, 2, 0);
        order.verify(eventPublisher).publishMatchStatusChanged(MATCH_ID, MatchStatus.ENDED, "종료");
        order.verifyNoMoreInteractions();
    }

    @Test
    void 상태만_변경되면_커밋_이후_상태_이벤트만_발행한다() {
        givenMatch(MatchStatus.IN_PLAY);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "1-0"))).isTrue();

        then(eventPublisher).shouldHaveNoInteractions();
        afterCommit();
        then(eventPublisher).should().publishMatchStatusChanged(MATCH_ID, MatchStatus.ENDED, "종료");
        then(eventPublisher).shouldHaveNoMoreInteractions();
    }

    @Test
    void 롤백되면_이벤트를_발행하지_않는다() {
        givenMatch(MatchStatus.IN_PLAY);

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "2-0"))).isTrue();
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 기존_경기가_없으면_새로_생성하지_않는다() {
        given(matchRepository.findForUpdateByDataOriginAndApiMatchId(DataOrigin.BETS, EXTERNAL_MATCH_ID))
                .willReturn(Optional.empty());

        assertThat(processor.process(leagueTarget(SportCode.FOOTBALL), event(MatchStatus.ENDED, "2-0"))).isFalse();

        then(matchRepository).should().findForUpdateByDataOriginAndApiMatchId(DataOrigin.BETS, EXTERNAL_MATCH_ID);
        then(matchRepository).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private Match givenMatch(MatchStatus status) {
        Match match = Match.builder()
                .id(MATCH_ID)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId(EXTERNAL_MATCH_ID)
                .statusCode(status)
                .homeId(1L)
                .awayId(2L)
                .homeScore(1)
                .awayScore(0)
                .build();
        given(matchRepository.findForUpdateByDataOriginAndApiMatchId(DataOrigin.BETS, EXTERNAL_MATCH_ID))
                .willReturn(Optional.of(match));
        return match;
    }

    private void assertScore(Match match, int homeScore, int awayScore) {
        assertThat(match.getHomeScore()).isEqualTo(homeScore);
        assertThat(match.getAwayScore()).isEqualTo(awayScore);
    }

    private MatchSyncLeagueTarget leagueTarget(SportCode sportCode) {
        return new MatchSyncLeagueTarget(DataOrigin.BETS, "1", "100", 1L, sportCode, 2L, "리그");
    }

    private BetsEventResponse.Event baseballEvent() {
        BetsEventResponse.Event event = event(MatchStatus.ENDED, "4-1");
        BetsEventResponse.Team home = new BetsEventResponse.Team();
        home.setId("A");
        BetsEventResponse.Team away = new BetsEventResponse.Team();
        away.setId("B");
        event.setHome(home);
        event.setAway(away);
        return event;
    }

    private BetsEventResponse.Event event(MatchStatus status, String ss) {
        BetsEventResponse.League league = new BetsEventResponse.League();
        league.setId("100");

        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId(EXTERNAL_MATCH_ID);
        event.setLeague(league);
        event.setTimeStatus(status.getCode());
        event.setSs(ss);
        return event;
    }

    private Team team(Long id) {
        return Team.builder().id(id).build();
    }

    private void afterCommit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
