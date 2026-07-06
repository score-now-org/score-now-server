package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MatchClockSyncServiceTest {

    private static final Long MATCH_ID = 1L;

    @InjectMocks
    private MatchClockSyncService service;

    @Mock
    private MatchDetailRepository matchDetailRepository;

    @Mock
    private MatchRealtimeEventPublisher eventPublisher;

    @Test
    void 자동_동기화에서_기존_시간정보가_없으면_저장하고_경기_시각_변경_이벤트를_발행한다() {
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.FIRST_HALF, 1, 0, true, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        service.syncMatchClock(MATCH_ID, newClock);

        then(matchDetailRepository).should().upsertMatchClock(MATCH_ID, newClock);
        then(eventPublisher).should().publishMatchClockChanged(MATCH_ID, newClock);
    }

    @Test
    void 자동_동기화에서_경과시간만_바뀌면_저장만_하고_경기_시각_변경_이벤트는_발행하지_않는다() {
        MatchDetailDocument.MatchClock currentClock = createMatchClock(MatchPeriod.FIRST_HALF, 10, 0, true, 0);
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.FIRST_HALF, 11, 0, true, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(createMatchDetail(currentClock)));

        service.syncMatchClock(MATCH_ID, newClock);

        then(matchDetailRepository).should().upsertMatchClock(MATCH_ID, newClock);
        then(eventPublisher).should(never()).publishMatchClockChanged(anyLong(), any());
    }

    @Test
    void 자동_동기화에서_구간이_바뀌면_경기_시각_변경_이벤트를_발행한다() {
        MatchDetailDocument.MatchClock currentClock = createMatchClock(MatchPeriod.FIRST_HALF, 45, 0, true, 0);
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, false, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(createMatchDetail(currentClock)));

        service.syncMatchClock(MATCH_ID, newClock);

        then(eventPublisher).should().publishMatchClockChanged(MATCH_ID, newClock);
    }

    @Test
    void 자동_동기화에서_running이_바뀌면_경기_시각_변경_이벤트를_발행한다() {
        MatchDetailDocument.MatchClock currentClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, false, 0);
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, true, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(createMatchDetail(currentClock)));

        service.syncMatchClock(MATCH_ID, newClock);

        then(eventPublisher).should().publishMatchClockChanged(MATCH_ID, newClock);
    }

    @Test
    void 수동_동기화에서_경과시간만_바뀌어도_경기_시각_변경_이벤트를_발행한다() {
        MatchDetailDocument.MatchClock currentClock = createMatchClock(MatchPeriod.FIRST_HALF, 10, 0, true, 0);
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.FIRST_HALF, 11, 0, true, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(createMatchDetail(currentClock)));

        service.syncManualMatchClock(MATCH_ID, newClock);

        then(eventPublisher).should().publishMatchClockChanged(MATCH_ID, newClock);
    }

    @Test
    void 수동_동기화에서_구간만_바뀌어도_경기_시각_변경_이벤트를_발행한다() {
        MatchDetailDocument.MatchClock currentClock = createMatchClock(MatchPeriod.FIRST_HALF, 45, 0, true, 0);
        MatchDetailDocument.MatchClock newClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, true, 0);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(createMatchDetail(currentClock)));

        service.syncManualMatchClock(MATCH_ID, newClock);

        then(eventPublisher).should().publishMatchClockChanged(MATCH_ID, newClock);
    }

    private MatchDetailDocument createMatchDetail(MatchDetailDocument.MatchClock matchClock) {
        return MatchDetailDocument.builder()
                .id(MATCH_ID)
                .matchClock(matchClock)
                .build();
    }

    private MatchDetailDocument.MatchClock createMatchClock(
            MatchPeriod period,
            Integer elapsedMinutes,
            Integer elapsedSeconds,
            Boolean running,
            Integer additionalMinutes
    ) {
        return MatchDetailDocument.MatchClock.builder()
                .period(period)
                .elapsedMinutes(elapsedMinutes)
                .elapsedSeconds(elapsedSeconds)
                .running(running)
                .additionalMinutes(additionalMinutes)
                .build();
    }
}
