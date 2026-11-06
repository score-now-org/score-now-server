package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndedMatchSyncServiceTest {

    @InjectMocks
    private EndedMatchSyncService service;

    @Mock
    private BetsApiClient betsApiClient;
    @Mock
    private MatchSyncLeagueTargetReader matchSyncLeagueTargetReader;
    @Mock
    private EndedMatchSyncProcessor endedMatchSyncProcessor;

    @Test
    void UTC_전일과_당일의_Ended_API_응답을_경기별_Processor로_전달한다() {
        MatchSyncLeagueTarget leagueTarget = leagueTarget();
        BetsEventResponse.Event event = event();
        given(matchSyncLeagueTargetReader.findEnabledLeagueTargets(DataOrigin.BETS))
                .willReturn(List.of(leagueTarget));
        given(betsApiClient.getEndedEvents(eq("16"), eq("100"), anyString()))
                .willReturn(response(event));
        given(endedMatchSyncProcessor.process(leagueTarget, event))
                .willReturn(true);

        int updated = service.syncRecent();

        assertThat(updated).isEqualTo(2);
        then(betsApiClient).should(times(2))
                .getEndedEvents(eq("16"), eq("100"), anyString());
        then(endedMatchSyncProcessor).should(times(2)).process(leagueTarget, event);
    }

    @Test
    void 상태가_변경된_경기만_갱신_건수에_포함한다() {
        MatchSyncLeagueTarget leagueTarget = leagueTarget();
        BetsEventResponse.Event event = event();
        given(matchSyncLeagueTargetReader.findEnabledLeagueTargets(DataOrigin.BETS))
                .willReturn(List.of(leagueTarget));
        given(betsApiClient.getEndedEvents(eq("16"), eq("100"), anyString()))
                .willReturn(response(event));
        given(endedMatchSyncProcessor.process(leagueTarget, event))
                .willReturn(false);

        int updated = service.syncRecent();

        assertThat(updated).isZero();
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

    private BetsEventResponse.Event event() {
        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId("api-1000");
        return event;
    }

    private BetsEventResponse response(BetsEventResponse.Event event) {
        BetsEventResponse response = new BetsEventResponse();
        response.setResults(List.of(event));
        return response;
    }
}
