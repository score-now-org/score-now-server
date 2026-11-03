package com.scorenow.scorenow_api.external.betsapi.normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventOddsResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetsEventOddsNormalizerTest {

    private final BetsEventOddsNormalizer normalizer = new BetsEventOddsNormalizer();

    @Test
    void 축구_승무패의_모든_프리매치_row를_시간순으로_자동_해외_배당률로_변환한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of("1_1", List.of(
                        winDrawLossRow("100", "1.50", "3.10", "2.40", "100", null),
                        winDrawLossRow("101", "1.239", "3.456", "2.345", "200", null)
                ))
        );

        List<MatchOddsHistory> result = normalizer.normalize(10L, SportCode.FOOTBALL, response);

        assertThat(result)
                .extracting(MatchOddsHistory::getApiOddsId)
                .containsExactly("100", "101");
        MatchOddsHistory history = result.get(1);
        assertThat(history.getMatchId()).isEqualTo(10L);
        assertThat(history.getCategory()).isEqualTo(OddsCategory.OVERSEAS);
        assertThat(history.getMarketType()).isEqualTo(OddsMarketType.WIN_DRAW_LOSS);
        assertThat(history.getApiOddsId()).isEqualTo("101");
        assertThat(history.getOrigin().isAutomatic()).isTrue();
        assertThat(history.getOrigin().getSource()).isEqualTo(OddsSource.BETS_API);
        assertThat(history.getOrigin().getBookmaker()).isEqualTo(OddsBookmaker.BET365);
        assertThat(history.getHomeOdds().value()).isEqualByComparingTo("1.23");
        assertThat(history.getDrawOdds().value()).isEqualByComparingTo("3.45");
        assertThat(history.getAwayOdds().value()).isEqualByComparingTo("2.34");
        assertThat(history.getPublishedAt()).isEqualTo(Instant.ofEpochSecond(200));
    }

    @Test
    void add_time이_같아도_apiOddsId가_다르면_모두_변환한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of("1_1", List.of(
                        winDrawLossRow("801", "1.80", "3.10", "2.00", "800", null),
                        winDrawLossRow("802", "1.85", "3.05", "1.95", "800", null)
                ))
        );

        List<MatchOddsHistory> result = normalizer.normalize(81L, SportCode.FOOTBALL, response);

        assertThat(result)
                .extracting(MatchOddsHistory::getApiOddsId)
                .containsExactly("801", "802");
        assertThat(result)
                .extracting(history -> history.getHomeOdds().value())
                .containsExactly(new BigDecimal("1.80"), new BigDecimal("1.85"));
        assertThat(result)
                .extracting(MatchOddsHistory::getPublishedAt)
                .containsOnly(Instant.ofEpochSecond(800));
    }

    @Test
    void 야구_matching_dir이_1이면_승패와_핸디캡의_홈원정을_교환하고_핸디캡_부호를_반전한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of(
                        "16_1", List.of(winDrawLossRow(
                                "201", "1.645", "3.00", "2.150", "300", null
                        )),
                        "16_2", List.of(handicapRow(
                                "202", "1.952", "-1.5", "1.740", "300", null
                        ))
                )
        );

        List<MatchOddsHistory> result = normalizer.normalize(20L, SportCode.BASEBALL, response);

        MatchOddsHistory winLoss = historyOf(result, OddsMarketType.WIN_DRAW_LOSS);
        assertThat(winLoss.getHomeOdds().value()).isEqualByComparingTo("2.15");
        assertThat(winLoss.getDrawOdds()).isNull();
        assertThat(winLoss.getAwayOdds().value()).isEqualByComparingTo("1.64");

        MatchOddsHistory handicap = historyOf(result, OddsMarketType.HANDICAP);
        assertThat(handicap.getHomeOdds().value()).isEqualByComparingTo("1.74");
        assertThat(handicap.getHandicapLine().value()).isEqualByComparingTo("1.50");
        assertThat(handicap.getAwayOdds().value()).isEqualByComparingTo("1.95");
    }

    @Test
    void 야구_matching_dir이_minus_1이면_승패와_핸디캡의_방향을_그대로_사용한다() {
        BetsEventOddsResponse response = response(
                -1,
                Map.of(
                        "16_1", List.of(winDrawLossRow(
                                "301", "2.11", null, "1.62", "400", null
                        )),
                        "16_2", List.of(handicapRow(
                                "302", "1.70", "1.5", "2.01", "400", null
                        ))
                )
        );

        List<MatchOddsHistory> result = normalizer.normalize(30L, SportCode.BASEBALL, response);

        MatchOddsHistory winLoss = historyOf(result, OddsMarketType.WIN_DRAW_LOSS);
        assertThat(winLoss.getHomeOdds().value()).isEqualByComparingTo("2.11");
        assertThat(winLoss.getAwayOdds().value()).isEqualByComparingTo("1.62");

        MatchOddsHistory handicap = historyOf(result, OddsMarketType.HANDICAP);
        assertThat(handicap.getHomeOdds().value()).isEqualByComparingTo("1.70");
        assertThat(handicap.getHandicapLine().value()).isEqualByComparingTo("1.50");
        assertThat(handicap.getAwayOdds().value()).isEqualByComparingTo("2.01");
    }

    @Test
    void 문자열로_제공된_matching_dir도_정수로_역직렬화하여_처리한다() throws Exception {
        String json = """
                {
                  "success": 1,
                  "results": {
                    "stats": {
                      "matching_dir": "-1",
                      "odds_update": {}
                    },
                    "odds": {
                      "16_1": [
                        {
                          "id": "351",
                          "home_od": "2.11",
                          "away_od": "1.62",
                          "add_time": "450"
                        }
                      ]
                    }
                  }
                }
                """;
        BetsEventOddsResponse response = new ObjectMapper().readValue(json, BetsEventOddsResponse.class);

        List<MatchOddsHistory> result = normalizer.normalize(35L, SportCode.BASEBALL, response);

        assertThat(response.results().stats().matchingDir()).isEqualTo(-1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHomeOdds().value()).isEqualByComparingTo("2.11");
        assertThat(result.get(0).getAwayOdds().value()).isEqualByComparingTo("1.62");
    }

    @Test
    void 축구_matching_dir이_minus_1이면_홈원정을_교환하고_핸디캡_부호를_반전한다() {
        BetsEventOddsResponse response = response(
                -1,
                Map.of(
                        "1_1", List.of(winDrawLossRow(
                                "401", "2.20", "3.10", "1.70", "500", null
                        )),
                        "1_2", List.of(handicapRow(
                                "402", "2.00", "1.5", "1.80", "500", null
                        ))
                )
        );

        List<MatchOddsHistory> result = normalizer.normalize(40L, SportCode.FOOTBALL, response);

        MatchOddsHistory winDrawLoss = historyOf(result, OddsMarketType.WIN_DRAW_LOSS);
        assertThat(winDrawLoss.getHomeOdds().value()).isEqualByComparingTo("1.70");
        assertThat(winDrawLoss.getDrawOdds().value()).isEqualByComparingTo("3.10");
        assertThat(winDrawLoss.getAwayOdds().value()).isEqualByComparingTo("2.20");

        MatchOddsHistory handicap = historyOf(result, OddsMarketType.HANDICAP);
        assertThat(handicap.getHomeOdds().value()).isEqualByComparingTo("1.80");
        assertThat(handicap.getHandicapLine().value()).isEqualByComparingTo("-1.50");
        assertThat(handicap.getAwayOdds().value()).isEqualByComparingTo("2.00");
    }

    @Test
    void 언더오버는_정수와_라이브_row를_제외한_가장_최신_row를_선택한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of("16_3", List.of(
                        underOverRow("501", "1.80", "2.5", "2.00", "100", null),
                        underOverRow("502", "1.888", "3.5", "1.999", "150", null),
                        underOverRow("503", "1.85", "4.0", "1.95", "200", null),
                        underOverRow("504", "1.90", "4.5", "1.90", "300", "1-0")
                ))
        );

        List<MatchOddsHistory> result = normalizer.normalize(50L, SportCode.BASEBALL, response);

        assertThat(result)
                .extracting(MatchOddsHistory::getApiOddsId)
                .containsExactly("501", "502");
        MatchOddsHistory history = result.get(1);
        assertThat(history.getApiOddsId()).isEqualTo("502");
        assertThat(history.getUnderOverLine().value()).isEqualByComparingTo("3.50");
        assertThat(history.getOverOdds().value()).isEqualByComparingTo("1.88");
        assertThat(history.getUnderOdds().value()).isEqualByComparingTo("1.99");
        assertThat(history.getPublishedAt()).isEqualTo(Instant.ofEpochSecond(150));
    }

    @Test
    void 동일한_apiOddsId가_중복되면_한_건만_변환한다() {
        BetsEventOddsResponse.OddsRecord duplicated = winDrawLossRow(
                "551", "1.80", "3.10", "2.00", "550", null
        );
        BetsEventOddsResponse response = response(
                1,
                Map.of("1_1", List.of(duplicated, duplicated))
        );

        List<MatchOddsHistory> result = normalizer.normalize(55L, SportCode.FOOTBALL, response);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApiOddsId()).isEqualTo("551");
    }

    @Test
    void 동일한_apiOddsId의_값이_서로_다르면_해당_row를_제외한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of("1_1", List.of(
                        winDrawLossRow("561", "1.80", "3.10", "2.00", "560", null),
                        winDrawLossRow("561", "1.85", "3.05", "1.95", "560", null)
                ))
        );

        List<MatchOddsHistory> result = normalizer.normalize(56L, SportCode.FOOTBALL, response);

        assertThat(result).isEmpty();
    }

    @Test
    void 잘못된_row가_있어도_같은_마켓과_다른_마켓의_유효한_row는_정상적으로_변환한다() {
        BetsEventOddsResponse response = response(
                1,
                Map.of(
                        "1_1", List.of(
                                winDrawLossRow("600", "1.80", "3.10", "2.00", "500", null),
                                winDrawLossRow("601", "-", "3.10", "2.00", "600", null)
                        ),
                        "1_3", List.of(underOverRow(
                                "602", "1.90", "2.5", "1.90", "600", null
                        ))
                )
        );

        List<MatchOddsHistory> result = normalizer.normalize(60L, SportCode.FOOTBALL, response);

        assertThat(result)
                .extracting(MatchOddsHistory::getMarketType)
                .containsExactly(OddsMarketType.WIN_DRAW_LOSS, OddsMarketType.UNDER_OVER);
        assertThat(result)
                .extracting(MatchOddsHistory::getApiOddsId)
                .containsExactly("600", "602");
    }

    @Test
    void matching_dir을_해석할_수_없으면_팀_방향_마켓만_제외한다() {
        BetsEventOddsResponse response = response(
                0,
                Map.of(
                        "1_1", List.of(winDrawLossRow(
                                "701", "1.70", "3.10", "2.20", "700", null
                        )),
                        "1_2", List.of(handicapRow(
                                "702", "1.90", "-0.5", "1.90", "700", null
                        )),
                        "1_3", List.of(underOverRow(
                                "703", "1.90", "2.5", "1.90", "700", null
                        ))
                )
        );

        List<MatchOddsHistory> result = normalizer.normalize(70L, SportCode.FOOTBALL, response);

        assertThat(result)
                .extracting(MatchOddsHistory::getMarketType)
                .containsExactly(OddsMarketType.UNDER_OVER);
    }

    @Test
    void 성공하지_않은_응답은_정규화하지_않는다() {
        BetsEventOddsResponse response = new BetsEventOddsResponse(0, null);

        assertThatThrownBy(() -> normalizer.normalize(80L, SportCode.FOOTBALL, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BetsAPI 배당률 응답 구조가 올바르지 않습니다.");
    }

    private MatchOddsHistory historyOf(List<MatchOddsHistory> histories, OddsMarketType marketType) {
        return histories.stream()
                .filter(history -> history.getMarketType() == marketType)
                .findFirst()
                .orElseThrow();
    }

    private BetsEventOddsResponse response(
            Integer matchingDirection,
            Map<String, List<BetsEventOddsResponse.OddsRecord>> odds
    ) {
        return new BetsEventOddsResponse(
                1,
                new BetsEventOddsResponse.Results(
                        new BetsEventOddsResponse.Stats(matchingDirection, Map.of()),
                        odds
                )
        );
    }

    private BetsEventOddsResponse.OddsRecord winDrawLossRow(
            String id,
            String homeOdds,
            String drawOdds,
            String awayOdds,
            String addTime,
            String score
    ) {
        return new BetsEventOddsResponse.OddsRecord(
                id,
                homeOdds,
                drawOdds,
                awayOdds,
                null,
                null,
                null,
                addTime,
                score,
                null
        );
    }

    private BetsEventOddsResponse.OddsRecord handicapRow(
            String id,
            String homeOdds,
            String handicap,
            String awayOdds,
            String addTime,
            String score
    ) {
        return new BetsEventOddsResponse.OddsRecord(
                id,
                homeOdds,
                null,
                awayOdds,
                handicap,
                null,
                null,
                addTime,
                score,
                null
        );
    }

    private BetsEventOddsResponse.OddsRecord underOverRow(
            String id,
            String overOdds,
            String handicap,
            String underOdds,
            String addTime,
            String score
    ) {
        return new BetsEventOddsResponse.OddsRecord(
                id,
                null,
                null,
                null,
                handicap,
                overOdds,
                underOdds,
                addTime,
                score,
                null
        );
    }
}
