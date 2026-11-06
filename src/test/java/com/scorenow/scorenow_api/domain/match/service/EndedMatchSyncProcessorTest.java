package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EndedMatchSyncProcessorTest {

    @InjectMocks
    private EndedMatchSyncProcessor processor;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;

    @Test
    void 기존_경기의_상태만_종료로_변경한다() {
        Match match = Match.builder()
                .id(10L)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId("api-1000")
                .homeId(1L)
                .awayId(2L)
                .homeScore(3)
                .awayScore(2)
                .startAt(LocalDateTime.of(2026, 11, 5, 18, 30))
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        BetsEventResponse.Event event = eventWithStatus(MatchStatus.ENDED);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.of(match));

        boolean updated = processor.process(leagueTarget(), event);

        assertThat(updated).isTrue();
        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertThat(match.getHomeId()).isEqualTo(1L);
        assertThat(match.getAwayId()).isEqualTo(2L);
        assertThat(match.getHomeScore()).isEqualTo(3);
        assertThat(match.getAwayScore()).isEqualTo(2);
        assertThat(match.getStartAt()).isEqualTo(LocalDateTime.of(2026, 11, 5, 18, 30));
        then(eventPublisher).should()
                .publishMatchStatusChanged(10L, MatchStatus.ENDED, "종료");
    }

    @Test
    void 이미_종료된_경기는_변경하지_않는다() {
        Match match = Match.builder()
                .id(10L)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId("api-1000")
                .statusCode(MatchStatus.ENDED)
                .build();
        BetsEventResponse.Event event = eventWithStatus(MatchStatus.ENDED);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.of(match));

        boolean updated = processor.process(leagueTarget(), event);

        assertThat(updated).isFalse();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void Ended_API의_연기_상태를_그대로_반영한다() {
        Match match = Match.builder()
                .id(10L)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId("api-1000")
                .statusCode(MatchStatus.ENDED)
                .build();
        BetsEventResponse.Event event = eventWithStatus(MatchStatus.POSTPONED);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.of(match));

        boolean updated = processor.process(leagueTarget(), event);

        assertThat(updated).isTrue();
        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.POSTPONED);
        then(eventPublisher).should()
                .publishMatchStatusChanged(10L, MatchStatus.POSTPONED, "연기");
    }

    @Test
    void Ended_API의_상태가_기존과_같으면_이벤트를_다시_발행하지_않는다() {
        Match match = Match.builder()
                .id(10L)
                .dataOrigin(DataOrigin.BETS)
                .apiMatchId("api-1000")
                .statusCode(MatchStatus.POSTPONED)
                .build();
        BetsEventResponse.Event event = eventWithStatus(MatchStatus.POSTPONED);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.of(match));

        boolean updated = processor.process(leagueTarget(), event);

        assertThat(updated).isFalse();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 기존_경기가_없으면_새로_생성하지_않는다() {
        BetsEventResponse.Event event = eventWithStatus(MatchStatus.ENDED);
        given(matchRepository.findByExternalInfo(DataOrigin.BETS, event.getId()))
                .willReturn(Optional.empty());

        boolean updated = processor.process(leagueTarget(), event);

        assertThat(updated).isFalse();
        then(matchRepository).should().findByExternalInfo(DataOrigin.BETS, event.getId());
        then(matchRepository).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private MatchSyncLeagueTarget leagueTarget() {
        return new MatchSyncLeagueTarget(
                DataOrigin.BETS,
                "16",
                "100",
                1L,
                SportCode.BASEBALL,
                2L,
                "KBO"
        );
    }

    private BetsEventResponse.Event eventWithStatus(MatchStatus status) {
        BetsEventResponse.League league = new BetsEventResponse.League();
        league.setId("100");

        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId("api-1000");
        event.setLeague(league);
        event.setTimeStatus(status.getCode());
        return event;
    }
}
