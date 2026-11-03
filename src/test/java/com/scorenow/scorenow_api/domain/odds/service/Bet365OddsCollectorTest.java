package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventOddsResponse;
import com.scorenow.scorenow_api.external.betsapi.normalizer.BetsEventOddsNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class Bet365OddsCollectorTest {

    @InjectMocks
    private Bet365OddsCollector collector;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private Bet365OddsPersistenceService persistenceService;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private BetsEventOddsNormalizer normalizer;

    @Test
    void 저장된_마켓들의_가장_오래된_최신_시각을_since_time으로_그대로_사용한다() {
        long matchId = 1L;
        String apiMatchId = "123456";
        Instant sinceTime = Instant.parse("2026-11-03T01:00:00Z");
        Match match = match(matchId, apiMatchId, SportCode.FOOTBALL);
        BetsEventOddsResponse response = response();
        List<MatchOddsHistory> histories = List.of();

        given(matchRepository.findByIdWithSport(matchId)).willReturn(Optional.of(match));
        given(persistenceService.resolveSinceTime(matchId)).willReturn(sinceTime);
        given(betsApiClient.getBet365EventOdds(apiMatchId, sinceTime)).willReturn(response);
        given(normalizer.normalize(matchId, SportCode.FOOTBALL, response)).willReturn(histories);
        given(persistenceService.saveNewHistories(matchId, histories)).willReturn(2);

        int savedCount = collector.collect(matchId);

        assertThat(savedCount).isEqualTo(2);
        then(betsApiClient).should().getBet365EventOdds(apiMatchId, sinceTime);
        then(normalizer).should().normalize(matchId, SportCode.FOOTBALL, response);
        then(persistenceService).should().saveNewHistories(matchId, histories);
    }

    @Test
    void 기존_자동_배당률이_없으면_since_time을_생략한다() {
        long matchId = 2L;
        String apiMatchId = "234567";
        Match match = match(matchId, apiMatchId, SportCode.BASEBALL);
        BetsEventOddsResponse response = response();

        given(matchRepository.findByIdWithSport(matchId)).willReturn(Optional.of(match));
        given(persistenceService.resolveSinceTime(matchId)).willReturn(null);
        given(betsApiClient.getBet365EventOdds(apiMatchId, null)).willReturn(response);
        given(normalizer.normalize(matchId, SportCode.BASEBALL, response)).willReturn(List.of());

        int savedCount = collector.collect(matchId);

        assertThat(savedCount).isZero();
        then(betsApiClient).should().getBet365EventOdds(apiMatchId, null);
        then(persistenceService).should().saveNewHistories(matchId, List.of());
    }

    @Test
    void 외부_API_호출이_실패하면_정규화와_저장을_진행하지_않는다() {
        long matchId = 3L;
        String apiMatchId = "345678";
        Match match = match(matchId, apiMatchId, SportCode.FOOTBALL);
        RuntimeException apiException = new RuntimeException("API 호출 실패");

        given(matchRepository.findByIdWithSport(matchId)).willReturn(Optional.of(match));
        given(persistenceService.resolveSinceTime(matchId)).willReturn(null);
        given(betsApiClient.getBet365EventOdds(apiMatchId, null)).willThrow(apiException);

        assertThatThrownBy(() -> collector.collect(matchId))
                .isSameAs(apiException);

        then(normalizer).shouldHaveNoInteractions();
        then(persistenceService).should().resolveSinceTime(matchId);
        then(persistenceService).shouldHaveNoMoreInteractions();
    }

    @Test
    void 잘못된_응답으로_정규화가_실패하면_저장하지_않는다() {
        long matchId = 4L;
        String apiMatchId = "456789";
        Match match = match(matchId, apiMatchId, SportCode.FOOTBALL);
        BetsEventOddsResponse response = response();
        IllegalArgumentException normalizeException = new IllegalArgumentException("잘못된 응답");

        given(matchRepository.findByIdWithSport(matchId)).willReturn(Optional.of(match));
        given(persistenceService.resolveSinceTime(matchId)).willReturn(null);
        given(betsApiClient.getBet365EventOdds(apiMatchId, null)).willReturn(response);
        given(normalizer.normalize(matchId, SportCode.FOOTBALL, response)).willThrow(normalizeException);

        assertThatThrownBy(() -> collector.collect(matchId))
                .isSameAs(normalizeException);

        then(persistenceService).should().resolveSinceTime(matchId);
        then(persistenceService).shouldHaveNoMoreInteractions();
    }

    private Match match(Long matchId, String apiMatchId, SportCode sportCode) {
        Sport sport = Sport.builder()
                .id(10L)
                .sportCode(sportCode)
                .build();

        return Match.builder()
                .id(matchId)
                .apiMatchId(apiMatchId)
                .sportId(sport.getId())
                .sport(sport)
                .build();
    }

    private BetsEventOddsResponse response() {
        return new BetsEventOddsResponse(
                1,
                new BetsEventOddsResponse.Results(
                        new BetsEventOddsResponse.Stats(1, Map.of()),
                        Map.of()
                )
        );
    }
}
