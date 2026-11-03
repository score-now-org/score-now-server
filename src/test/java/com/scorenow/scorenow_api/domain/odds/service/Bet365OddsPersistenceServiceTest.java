package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsHistoryRepository;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class Bet365OddsPersistenceServiceTest {

    private static final OddsOrigin BET365_ORIGIN = OddsOrigin.automatic(
            OddsSource.BETS_API,
            OddsBookmaker.BET365
    );

    @InjectMocks
    private Bet365OddsPersistenceService persistenceService;

    @Mock
    private MatchOddsHistoryRepository matchOddsHistoryRepository;

    @Test
    void 존재하는_마켓별_최신_시각_중_가장_오래된_시각을_반환한다() {
        long matchId = 1L;
        Instant winDrawLossLatest = Instant.parse("2026-11-03T01:30:00Z");
        Instant handicapLatest = Instant.parse("2026-11-03T01:00:00Z");

        given(matchOddsHistoryRepository.findLatestPublishedAtPerMarket(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                OddsMarketType.getAllTypes()
        )).willReturn(List.of(winDrawLossLatest, handicapLatest));

        Instant sinceTime = persistenceService.resolveSinceTime(matchId);

        assertThat(sinceTime).isEqualTo(handicapLatest);
    }

    @Test
    void 저장된_자동_Bet365_마켓이_없으면_since_time은_null이다() {
        long matchId = 2L;

        given(matchOddsHistoryRepository.findLatestPublishedAtPerMarket(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                OddsMarketType.getAllTypes()
        )).willReturn(List.of());

        assertThat(persistenceService.resolveSinceTime(matchId)).isNull();
    }

    @Test
    void 새로운_apiOddsId는_배당률이_기존과_같아도_저장한다() {
        long matchId = 3L;
        MatchOddsHistory existing = winDrawLoss(matchId, "100", "1.80", "3.10", "2.10");
        MatchOddsHistory alreadyCollected = winDrawLoss(matchId, "100", "1.80", "3.10", "2.10");
        MatchOddsHistory newProviderRow = winDrawLoss(matchId, "101", "1.80", "3.10", "2.10");

        given(matchOddsHistoryRepository.findAllByProviderOddsIds(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                Set.of("100", "101")
        )).willReturn(List.of(existing));

        int savedCount = persistenceService.saveNewHistories(
                matchId,
                List.of(alreadyCollected, newProviderRow)
        );

        assertThat(savedCount).isEqualTo(1);
        ArgumentCaptor<List<MatchOddsHistory>> captor = ArgumentCaptor.forClass(List.class);
        then(matchOddsHistoryRepository).should().saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(MatchOddsHistory::getApiOddsId)
                .containsExactly("101");
    }

    @Test
    void 새로운_apiOddsId이면_배당률이나_기준점이_변경된_row도_저장한다() {
        long matchId = 4L;
        MatchOddsHistory changedOdds = winDrawLoss(matchId, "201", "1.90", "3.20", "2.00");
        MatchOddsHistory changedLine = handicap(matchId, "202", "1.95", "-1.50", "1.85");

        given(matchOddsHistoryRepository.findAllByProviderOddsIds(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                Set.of("201", "202")
        )).willReturn(List.of());

        int savedCount = persistenceService.saveNewHistories(matchId, List.of(changedOdds, changedLine));

        assertThat(savedCount).isEqualTo(2);
        then(matchOddsHistoryRepository).should().saveAll(List.of(changedOdds, changedLine));
    }

    @Test
    void 동일한_마켓과_apiOddsId가_이미_존재하면_값이_달라도_저장하지_않는다() {
        long matchId = 5L;
        MatchOddsHistory existing = winDrawLoss(matchId, "300", "1.80", "3.10", "2.10");
        MatchOddsHistory sameProviderIdWithChangedOdds = winDrawLoss(
                matchId,
                "300",
                "1.90",
                "3.10",
                "2.10"
        );

        given(matchOddsHistoryRepository.findAllByProviderOddsIds(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                Set.of("300")
        )).willReturn(List.of(existing));

        int savedCount = persistenceService.saveNewHistories(
                matchId,
                List.of(sameProviderIdWithChangedOdds)
        );

        assertThat(savedCount).isZero();
        then(matchOddsHistoryRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void apiOddsId가_같아도_마켓이_다르면_별도_이력으로_저장한다() {
        long matchId = 6L;
        MatchOddsHistory existingWinDrawLoss = winDrawLoss(matchId, "400", "1.80", "3.10", "2.10");
        MatchOddsHistory newHandicap = handicap(matchId, "400", "1.90", "-1.50", "1.90");

        given(matchOddsHistoryRepository.findAllByProviderOddsIds(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                Set.of("400")
        )).willReturn(List.of(existingWinDrawLoss));

        int savedCount = persistenceService.saveNewHistories(matchId, List.of(newHandicap));

        assertThat(savedCount).isEqualTo(1);
        then(matchOddsHistoryRepository).should().saveAll(List.of(newHandicap));
    }

    @Test
    void 저장할_이력이_없으면_Repository를_호출하지_않는다() {
        assertThat(persistenceService.saveNewHistories(7L, List.of())).isZero();
        then(matchOddsHistoryRepository).shouldHaveNoInteractions();
    }

    private MatchOddsHistory winDrawLoss(
            Long matchId,
            String apiOddsId,
            String homeOdds,
            String drawOdds,
            String awayOdds
    ) {
        return MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                BET365_ORIGIN,
                new BigDecimal(homeOdds),
                new BigDecimal(drawOdds),
                new BigDecimal(awayOdds),
                apiOddsId,
                Instant.parse("2026-11-03T01:00:00Z")
        );
    }

    private MatchOddsHistory handicap(
            Long matchId,
            String apiOddsId,
            String homeOdds,
            String line,
            String awayOdds
    ) {
        return MatchOddsHistory.createHandicap(
                matchId,
                OddsCategory.OVERSEAS,
                BET365_ORIGIN,
                new BigDecimal(homeOdds),
                new BigDecimal(line),
                new BigDecimal(awayOdds),
                apiOddsId,
                Instant.parse("2026-11-03T01:00:00Z")
        );
    }
}
