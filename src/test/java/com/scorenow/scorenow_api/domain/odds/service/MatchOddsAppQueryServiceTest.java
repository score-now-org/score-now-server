package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.dto.cursor.MatchOddsHistoryCursor;
import com.scorenow.scorenow_api.domain.odds.dto.response.MatchOddsCategoryResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.MatchOddsHistorySliceResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.OddsCellResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.OddsMarketSnapshotResponse;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCellPosition;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCellType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsChangeDirection;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsDisplayPolicyRepository;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsHistoryRepository;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchOddsAppQueryServiceTest {

    @InjectMocks
    private MatchOddsAppQueryService matchOddsAppQueryService;

    @Mock
    private MatchOddsHistoryRepository matchOddsHistoryRepository;

    @Mock
    private MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository;

    @Test
    void 경기_ID가_없으면_해외_승무패_배당률을_조회하지_않는다() {
        Map<Long, MatchOddsCategoryResponse> result =
                matchOddsAppQueryService.getOverseasWinDrawLossOddsByMatchIds(Set.of());

        assertThat(result).isEmpty();
        then(matchOddsDisplayPolicyRepository).shouldHaveNoInteractions();
        then(matchOddsHistoryRepository).shouldHaveNoInteractions();
    }

    @Test
    void 자동_정책이면_Bet365의_최신_해외_승무패와_직전_배당률을_비교한다() {
        long matchId = 1L;
        OddsOrigin origin = OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        MatchOddsHistory currentHistory = createFootballWinDrawLoss(
                matchId,
                origin,
                "1.90",
                "3.10",
                "2.10",
                "2026-10-30T10:00:00Z"
        );
        MatchOddsHistory previousHistory = createFootballWinDrawLoss(
                matchId,
                origin,
                "1.80",
                "3.20",
                "2.10",
                "2026-10-30T09:00:00Z"
        );

        given(matchOddsDisplayPolicyRepository.findAllByMatchIdInAndCategory(
                Set.of(matchId),
                OddsCategory.OVERSEAS
        )).willReturn(List.of(policy));
        given(matchOddsHistoryRepository.findLatestTwoActivePerMatch(
                Set.of(matchId),
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                origin
        )).willReturn(List.of(currentHistory, previousHistory));

        Map<Long, MatchOddsCategoryResponse> result =
                matchOddsAppQueryService.getOverseasWinDrawLossOddsByMatchIds(Set.of(matchId));

        MatchOddsCategoryResponse response = result.get(matchId);
        assertThat(response.category()).isEqualTo(OddsCategory.OVERSEAS);
        assertThat(response.markets()).singleElement().satisfies(market -> {
            assertThat(market.marketType()).isEqualTo(OddsMarketType.WIN_DRAW_LOSS);
            assertThat(market.publishedAt()).isEqualTo(Instant.parse("2026-10-30T10:00:00Z"));
            assertThat(market.cells())
                    .extracting(
                            OddsCellResponse::position,
                            OddsCellResponse::type,
                            OddsCellResponse::currentValue,
                            OddsCellResponse::previousValue,
                            OddsCellResponse::changeDirection
                    )
                    .containsExactly(
                            tuple(OddsCellPosition.LEFT, OddsCellType.HOME,
                                    new BigDecimal("1.90"), new BigDecimal("1.80"), OddsChangeDirection.UP),
                            tuple(OddsCellPosition.CENTER, OddsCellType.DRAW,
                                    new BigDecimal("3.10"), new BigDecimal("3.20"), OddsChangeDirection.DOWN),
                            tuple(OddsCellPosition.RIGHT, OddsCellType.AWAY,
                                    new BigDecimal("2.10"), new BigDecimal("2.10"), OddsChangeDirection.SAME)
                    );
        });
    }

    @Test
    void 수동_정책의_야구_승패는_DRAW_셀_없이_홈과_원정만_반환한다() {
        long matchId = 2L;
        OddsOrigin origin = OddsOrigin.manual();
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        policy.changeDisplayedRegistrationType(OddsRegistrationType.MANUAL);
        MatchOddsHistory currentHistory = createBaseballWinDrawLoss(
                matchId,
                origin,
                "1.80",
                "2.00",
                "2026-10-30T10:00:00Z"
        );
        MatchOddsHistory previousHistory = createBaseballWinDrawLoss(
                matchId,
                origin,
                "1.90",
                "1.90",
                "2026-10-30T09:00:00Z"
        );

        given(matchOddsDisplayPolicyRepository.findAllByMatchIdInAndCategory(
                Set.of(matchId),
                OddsCategory.OVERSEAS
        )).willReturn(List.of(policy));
        given(matchOddsHistoryRepository.findLatestTwoActivePerMatch(
                Set.of(matchId),
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                origin
        )).willReturn(List.of(currentHistory, previousHistory));

        Map<Long, MatchOddsCategoryResponse> result =
                matchOddsAppQueryService.getOverseasWinDrawLossOddsByMatchIds(Set.of(matchId));

        OddsMarketSnapshotResponse market = result.get(matchId).markets().get(0);
        assertThat(market.cells())
                .extracting(OddsCellResponse::position, OddsCellResponse::type)
                .containsExactly(
                        tuple(OddsCellPosition.LEFT, OddsCellType.HOME),
                        tuple(OddsCellPosition.RIGHT, OddsCellType.AWAY)
                );
        then(matchOddsHistoryRepository).should().findLatestTwoActivePerMatch(
                Set.of(matchId),
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                origin
        );
        then(matchOddsHistoryRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void 핸디캡_첫_페이지는_요청_개수만_반환하고_최신값의_증감과_다음_커서를_제공한다() {
        long matchId = 3L;
        OddsOrigin origin = OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        MatchOddsHistory currentHistory = withId(createHandicap(
                matchId, origin, "1.90", "-1.50", "1.95", "2026-10-31T10:00:00Z"
        ), 30L);
        MatchOddsHistory previousHistory = withId(createHandicap(
                matchId, origin, "1.80", "-1.00", "2.00", "2026-10-31T09:00:00Z"
        ), 29L);
        MatchOddsHistory extraHistory = withId(createHandicap(
                matchId, origin, "1.75", "-0.50", "2.10", "2026-10-31T08:00:00Z"
        ), 28L);

        given(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .willReturn(Optional.of(policy));
        given(matchOddsHistoryRepository.findActiveHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsStatus.ACTIVE,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                null,
                null,
                PageRequest.of(0, 3)
        )).willReturn(List.of(currentHistory, previousHistory, extraHistory));

        MatchOddsHistorySliceResponse result = matchOddsAppQueryService.getMatchOddsHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                null,
                2
        );

        assertThat(result.rows()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(MatchOddsHistoryCursor.parse(
                result.nextCursor(),
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsRegistrationType.AUTOMATIC
        )).isEqualTo(new MatchOddsHistoryCursor(
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsRegistrationType.AUTOMATIC,
                previousHistory.getPublishedAt(),
                previousHistory.getId()
        ));
        assertThat(result.rows().get(0).cells())
                .extracting(
                        OddsCellResponse::type,
                        OddsCellResponse::currentValue,
                        OddsCellResponse::previousValue,
                        OddsCellResponse::changeDirection
                )
                .containsExactly(
                        tuple(OddsCellType.HOME, new BigDecimal("1.90"),
                                new BigDecimal("1.80"), OddsChangeDirection.UP),
                        tuple(OddsCellType.HANDICAP_LINE, new BigDecimal("-1.50"),
                                null, OddsChangeDirection.NONE),
                        tuple(OddsCellType.AWAY, new BigDecimal("1.95"),
                                new BigDecimal("2.00"), OddsChangeDirection.DOWN)
                );
        assertThat(result.rows().get(1).cells())
                .extracting(OddsCellResponse::changeDirection)
                .containsOnly(OddsChangeDirection.NONE);
    }

    @Test
    void 다음_페이지의_언더오버_이력은_증감을_계산하지_않는다() {
        long matchId = 4L;
        OddsOrigin origin = OddsOrigin.manual();
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        policy.changeDisplayedRegistrationType(OddsRegistrationType.MANUAL);
        MatchOddsHistory firstHistory = withId(createUnderOver(
                matchId, origin, "1.85", "8.50", "1.95", "2026-10-31T08:00:00Z"
        ), 40L);
        MatchOddsHistory secondHistory = withId(createUnderOver(
                matchId, origin, "1.80", "7.50", "2.00", "2026-10-31T07:00:00Z"
        ), 39L);
        MatchOddsHistoryCursor cursor = new MatchOddsHistoryCursor(
                OddsCategory.OVERSEAS,
                OddsMarketType.UNDER_OVER,
                OddsRegistrationType.MANUAL,
                Instant.parse("2026-10-31T09:00:00Z"),
                41L
        );

        given(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .willReturn(Optional.of(policy));
        given(matchOddsHistoryRepository.findActiveHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.UNDER_OVER,
                OddsStatus.ACTIVE,
                OddsRegistrationType.MANUAL,
                null,
                null,
                cursor.publishedAt(),
                cursor.historyId(),
                PageRequest.of(0, 3)
        )).willReturn(List.of(firstHistory, secondHistory));

        MatchOddsHistorySliceResponse result = matchOddsAppQueryService.getMatchOddsHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.UNDER_OVER,
                cursor.encode(),
                2
        );

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).cells())
                .extracting(OddsCellResponse::type)
                .containsExactly(OddsCellType.UNDER, OddsCellType.UNDER_OVER_LINE, OddsCellType.OVER);
        assertThat(result.rows().get(0).cells())
                .extracting(OddsCellResponse::changeDirection)
                .containsOnly(OddsChangeDirection.NONE);
    }

    @Test
    void 야구_승패_이력은_DRAW_셀_없이_반환한다() {
        long matchId = 5L;
        OddsOrigin origin = OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        MatchOddsHistory history = withId(createBaseballWinDrawLoss(
                matchId, origin, "1.80", "2.00", "2026-10-31T10:00:00Z"
        ), 50L);

        given(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .willReturn(Optional.of(policy));
        given(matchOddsHistoryRepository.findActiveHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsStatus.ACTIVE,
                OddsRegistrationType.AUTOMATIC,
                OddsSource.BETS_API,
                OddsBookmaker.BET365,
                null,
                null,
                PageRequest.of(0, 21)
        )).willReturn(List.of(history));

        MatchOddsHistorySliceResponse result = matchOddsAppQueryService.getMatchOddsHistorySlice(
                matchId,
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                null,
                20
        );

        assertThat(result.rows()).singleElement().satisfies(row ->
                assertThat(row.cells())
                        .extracting(OddsCellResponse::position, OddsCellResponse::type)
                        .containsExactly(
                                tuple(OddsCellPosition.LEFT, OddsCellType.HOME),
                                tuple(OddsCellPosition.RIGHT, OddsCellType.AWAY)
                        )
        );
    }

    private MatchOddsHistory createFootballWinDrawLoss(
            long matchId,
            OddsOrigin origin,
            String homeOdds,
            String drawOdds,
            String awayOdds,
            String publishedAt
    ) {
        return MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                origin,
                new BigDecimal(homeOdds),
                new BigDecimal(drawOdds),
                new BigDecimal(awayOdds),
                Instant.parse(publishedAt)
        );
    }

    private MatchOddsHistory createBaseballWinDrawLoss(
            long matchId,
            OddsOrigin origin,
            String homeOdds,
            String awayOdds,
            String publishedAt
    ) {
        return MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                origin,
                new BigDecimal(homeOdds),
                null,
                new BigDecimal(awayOdds),
                Instant.parse(publishedAt)
        );
    }

    private MatchOddsHistory createHandicap(
            long matchId,
            OddsOrigin origin,
            String homeOdds,
            String handicapLine,
            String awayOdds,
            String publishedAt
    ) {
        return MatchOddsHistory.createHandicap(
                matchId,
                OddsCategory.OVERSEAS,
                origin,
                new BigDecimal(homeOdds),
                new BigDecimal(handicapLine),
                new BigDecimal(awayOdds),
                Instant.parse(publishedAt)
        );
    }

    private MatchOddsHistory createUnderOver(
            long matchId,
            OddsOrigin origin,
            String underOdds,
            String underOverLine,
            String overOdds,
            String publishedAt
    ) {
        return MatchOddsHistory.createUnderOver(
                matchId,
                OddsCategory.OVERSEAS,
                origin,
                new BigDecimal(underOdds),
                new BigDecimal(underOverLine),
                new BigDecimal(overOdds),
                Instant.parse(publishedAt)
        );
    }

    private MatchOddsHistory withId(MatchOddsHistory history, long id) {
        ReflectionTestUtils.setField(history, "id", id);
        return history;
    }
}
