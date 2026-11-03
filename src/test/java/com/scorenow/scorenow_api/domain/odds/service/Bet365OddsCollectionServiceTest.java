package com.scorenow.scorenow_api.domain.odds.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE_ID;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class Bet365OddsCollectionServiceTest {

    @InjectMocks
    private Bet365OddsCollectionService collectionService;

    @Mock
    private PrematchOddsCollectionTargetReader targetReader;

    @Mock
    private Bet365OddsCollector collector;

    @Test
    void 경기_시작까지_1시간_초과_48시간_이하인_경기를_수집한다() {
        ZonedDateTime standardTime = ZonedDateTime.of(
                2026, 11, 4, 0, 0, 0, 0, ZoneOffset.UTC
        );
        LocalDateTime startExclusive = LocalDateTime.of(2026, 11, 4, 10, 0);
        LocalDateTime endInclusive = LocalDateTime.of(2026, 11, 6, 9, 0);

        given(targetReader.findTargets(startExclusive, endInclusive)).willReturn(List.of(1L, 2L));
        given(collector.collect(1L)).willReturn(2);
        given(collector.collect(2L)).willReturn(1);

        collectionService.collectDistantPrematchOdds(standardTime);

        then(targetReader).should().findTargets(startExclusive, endInclusive);
        then(collector).should().collect(1L);
        then(collector).should().collect(2L);
    }

    @Test
    void 경기_시작까지_1시간_이하로_남은_경기를_수집한다() {
        ZonedDateTime standardTime = ZonedDateTime.of(
                2026, 11, 4, 9, 0, 0, 0, SEOUL_TIME_ZONE_ID
        );
        LocalDateTime startExclusive = LocalDateTime.of(2026, 11, 4, 9, 0);
        LocalDateTime endInclusive = LocalDateTime.of(2026, 11, 4, 10, 0);

        given(targetReader.findTargets(startExclusive, endInclusive)).willReturn(List.of(3L));

        collectionService.collectNearStartPrematchOdds(standardTime);

        then(targetReader).should().findTargets(startExclusive, endInclusive);
        then(collector).should().collect(3L);
    }

    @Test
    void 일부_경기의_수집이_실패해도_나머지_경기를_계속_수집한다() {
        ZonedDateTime standardTime = ZonedDateTime.of(
                2026, 11, 4, 9, 0, 0, 0, SEOUL_TIME_ZONE_ID
        );
        LocalDateTime startExclusive = standardTime.toLocalDateTime();
        LocalDateTime endInclusive = standardTime.plusHours(1).toLocalDateTime();

        given(targetReader.findTargets(startExclusive, endInclusive)).willReturn(List.of(1L, 2L, 3L));
        given(collector.collect(1L)).willReturn(1);
        given(collector.collect(2L)).willThrow(new RuntimeException("외부 API 실패"));
        given(collector.collect(3L)).willReturn(2);

        collectionService.collectNearStartPrematchOdds(standardTime);

        InOrder inOrder = inOrder(collector);
        inOrder.verify(collector).collect(1L);
        inOrder.verify(collector).collect(2L);
        inOrder.verify(collector).collect(3L);
    }
}
