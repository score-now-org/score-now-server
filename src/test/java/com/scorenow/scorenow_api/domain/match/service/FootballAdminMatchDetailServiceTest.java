package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballStatsUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchScoreUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.FootballMatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FootballAdminMatchDetailServiceTest {

    private static final Long MATCH_ID = 1L;

    @InjectMocks
    private FootballAdminMatchDetailService service;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailRepository matchDetailRepository;
    @Mock
    private FootballMatchDetailRepository footballMatchDetailRepository;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;
    @Mock
    private SportDetailEventProcessorRegistry eventProcessorRegistry;
    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;
    @Mock
    private MatchScheduledStartAtUpdater matchScheduledStartAtUpdater;

    @Test
    void 양팀_점수가_모두_변경되어도_점수와_이벤트를_갱신한다() {
        Match match = Match.builder()
                .id(MATCH_ID)
                .homeScore(0)
                .awayScore(0)
                .build();
        MatchScoreUpdateRequest request = new MatchScoreUpdateRequest();
        ReflectionTestUtils.setField(request, "homeScore", 2);
        ReflectionTestUtils.setField(request, "awayScore", 1);
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));

        service.updateMatchScore(MATCH_ID, request);

        assertThat(match.getHomeScore()).isEqualTo(2);
        assertThat(match.getAwayScore()).isEqualTo(1);
        then(eventPublisher).should().publishScoreChanged(MATCH_ID, 2, 1);
    }

    @Test
    void 같은_점수면_이벤트를_발행하지_않는다() {
        Match match = Match.builder()
                .id(MATCH_ID)
                .homeScore(2)
                .awayScore(1)
                .build();
        MatchScoreUpdateRequest request = new MatchScoreUpdateRequest();
        ReflectionTestUtils.setField(request, "homeScore", 2);
        ReflectionTestUtils.setField(request, "awayScore", 1);
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));

        service.updateMatchScore(MATCH_ID, request);

        then(eventPublisher).should(never()).publishScoreChanged(MATCH_ID, 2, 1);
    }

    @Test
    void 스탯은_sportDetail_전체가_아니라_스탯_경로만_수정한다() {
        FootballStatsUpdateRequest request = createStatsRequest();
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(footballDocument(null)));

        service.updateFootballStats(MATCH_ID, request);

        ArgumentCaptor<FootballStats> homeCaptor = ArgumentCaptor.forClass(FootballStats.class);
        ArgumentCaptor<FootballStats> awayCaptor = ArgumentCaptor.forClass(FootballStats.class);
        then(footballMatchDetailRepository).should()
                .updateStats(org.mockito.ArgumentMatchers.eq(MATCH_ID), homeCaptor.capture(), awayCaptor.capture());
        assertThat(homeCaptor.getValue().getShots()).isEqualTo(10);
        assertThat(awayCaptor.getValue().getShots()).isNull();
        then(matchDetailRepository).should(never()).upsertMatchDetail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void Clock이_없으면_일시정지를_거절한다() {
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(footballDocument(FootballDetail.empty())));

        assertThatThrownBy(() -> service.pauseClock(MATCH_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_CLOCK_NOT_INITIALIZED));
    }

    @Test
    void 일시정지는_현재까지_흐른_시간을_누적하고_Clock_경로만_수정한다() {
        FootballClock clock = FootballClock.builder()
                .clockSyncedAt(Instant.now().minusSeconds(60))
                .phaseElapsedSeconds(30)
                .phase(FootballPhase.FIRST_HALF)
                .running(true)
                .build();
        MatchDetailDocument document = footballDocument(FootballDetail.builder().clock(clock).build());
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(document));

        service.pauseClock(MATCH_ID);

        ArgumentCaptor<FootballClock> clockCaptor = ArgumentCaptor.forClass(FootballClock.class);
        then(footballMatchDetailRepository).should().updateClock(org.mockito.ArgumentMatchers.eq(MATCH_ID), clockCaptor.capture());
        assertThat(clockCaptor.getValue().getRunning()).isFalse();
        assertThat(clockCaptor.getValue().getPhaseElapsedSeconds()).isGreaterThanOrEqualTo(90);
        then(eventProcessorRegistry).should().process(
                org.mockito.ArgumentMatchers.eq(document),
                org.mockito.ArgumentMatchers.any(MatchDetailDocument.class));
    }

    private FootballStatsUpdateRequest createStatsRequest() {
        FootballStatsUpdateRequest.TeamStats home = new FootballStatsUpdateRequest.TeamStats();
        FootballStatsUpdateRequest.TeamStats away = new FootballStatsUpdateRequest.TeamStats();
        ReflectionTestUtils.setField(home, "shots", 10);

        FootballStatsUpdateRequest request = new FootballStatsUpdateRequest();
        ReflectionTestUtils.setField(request, "homeStats", home);
        ReflectionTestUtils.setField(request, "awayStats", away);
        return request;
    }

    private MatchDetailDocument footballDocument(FootballDetail footballDetail) {
        return MatchDetailDocument.builder()
                .id(MATCH_ID)
                .type(SportDetailType.FOOTBALL)
                .sportDetail(footballDetail)
                .build();
    }
}
