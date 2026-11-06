package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpcomingMatchSyncServiceTest {

    @InjectMocks
    private UpcomingMatchSyncService service;

    @Mock
    private BetsApiClient betsApiClient;
    @Mock
    private MatchSyncLeagueTargetReader matchSyncLeagueTargetReader;
    @Mock
    private UpcomingMatchSyncProcessor upcomingMatchSyncProcessor;

    @Test
    void Upcoming_API_응답을_경기별_Processor로_전달한다() {
        MatchSyncLeagueTarget leagueTarget = leagueTarget();
        BetsEventResponse.Event event = event("api-1000");
        given(matchSyncLeagueTargetReader.findEnabledLeagueTargets(DataOrigin.BETS))
                .willReturn(List.of(leagueTarget));
        given(betsApiClient.getUpcomingEvents("16", "100", "20261106"))
                .willReturn(response(event));

        int synced = service.sync(LocalDate.of(2026, 11, 6));

        assertThat(synced).isEqualTo(1);
        then(upcomingMatchSyncProcessor).should().process(leagueTarget, event);
    }

    @Test
    void 한_경기_처리가_실패해도_다음_경기를_계속_처리한다() {
        MatchSyncLeagueTarget leagueTarget = leagueTarget();
        BetsEventResponse.Event first = event("api-1000");
        BetsEventResponse.Event second = event("api-1001");
        given(matchSyncLeagueTargetReader.findEnabledLeagueTargets(DataOrigin.BETS))
                .willReturn(List.of(leagueTarget));
        given(betsApiClient.getUpcomingEvents("16", "100", "20261106"))
                .willReturn(response(first, second));
        willThrow(new RuntimeException("동기화 실패"))
                .given(upcomingMatchSyncProcessor)
                .process(leagueTarget, first);

        int synced = service.sync(LocalDate.of(2026, 11, 6));

        assertThat(synced).isEqualTo(1);
        then(upcomingMatchSyncProcessor).should().process(leagueTarget, second);
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

    private BetsEventResponse.Event event(String id) {
        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId(id);
        return event;
    }

    private BetsEventResponse response(BetsEventResponse.Event... events) {
        BetsEventResponse response = new BetsEventResponse();
        response.setResults(List.of(events));
        return response;
    }
}
