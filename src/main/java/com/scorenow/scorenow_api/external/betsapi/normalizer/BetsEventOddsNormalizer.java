package com.scorenow.scorenow_api.external.betsapi.normalizer;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import com.scorenow.scorenow_api.domain.odds.value.UnderOverLine;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventOddsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Component
public class BetsEventOddsNormalizer {

    private static final int MATCHING_DIRECTION_SAME = 1;
    private static final int MATCHING_DIRECTION_REVERSED = -1;

    private static final String WIN_DRAW_LOSS_MARKET_SUFFIX = "_1";
    private static final String HANDICAP_MARKET_SUFFIX = "_2";
    private static final String UNDER_OVER_MARKET_SUFFIX = "_3";

    private static final Comparator<ParsedOddsRow> PARSED_ODDS_ROW_ORDER =
            Comparator.comparing(ParsedOddsRow::publishedAt)
                    .thenComparing(ParsedOddsRow::apiOddsId, BetsEventOddsNormalizer::compareApiOddsIds);

    private static final Comparator<MatchOddsHistory> HISTORY_ORDER =
            Comparator.comparing(MatchOddsHistory::getPublishedAt)
                    .thenComparing(MatchOddsHistory::getApiOddsId, BetsEventOddsNormalizer::compareApiOddsIds);

    public List<MatchOddsHistory> normalize(Long matchId, SportCode sportCode, BetsEventOddsResponse response) {
        validateInput(matchId, sportCode, response);

        List<MatchOddsHistory> histories = new ArrayList<>();
        Integer matchingDirection = resolveMatchingDirection(response);

        histories.addAll(normalizeWinDrawLoss(matchId, sportCode, matchingDirection, response));
        histories.addAll(normalizeHandicap(matchId, sportCode, matchingDirection, response));
        histories.addAll(normalizeUnderOver(matchId, response));
        histories.sort(HISTORY_ORDER);

        return List.copyOf(histories);
    }

    private List<MatchOddsHistory> normalizeWinDrawLoss(
            Long matchId,
            SportCode sportCode,
            Integer matchingDirection,
            BetsEventOddsResponse response
    ) {
        if (!isSupportedMatchingDirection(matchingDirection)) {
            log.warn(
                    "BetsAPI 승무패 정규화 스킵 - 지원하지 않는 matching_dir. matchId:{}, matchingDir:{}",
                    matchId,
                    matchingDirection
            );
            return List.of();
        }

        List<ParsedOddsRow> parsedOddsRows = findParsedOddsRows(
                matchId,
                response,
                WIN_DRAW_LOSS_MARKET_SUFFIX,
                ignored -> true
        );
        List<MatchOddsHistory> histories = new ArrayList<>();

        for (ParsedOddsRow parsedOddsRow : parsedOddsRows) {
            try {
                BetsEventOddsResponse.OddsRecord row = parsedOddsRow.row();
                BigDecimal homeOdds = parseDecimal(row.homeOdds(), "home_od");
                BigDecimal awayOdds = parseDecimal(row.awayOdds(), "away_od");
                BigDecimal drawOdds = sportCode == SportCode.FOOTBALL
                        ? parseDecimal(row.drawOdds(), "draw_od")
                        : null;

                if (shouldSwapSides(sportCode, matchingDirection)) {
                    BigDecimal originalHomeOdds = homeOdds;
                    homeOdds = awayOdds;
                    awayOdds = originalHomeOdds;
                }

                histories.add(MatchOddsHistory.createWinDrawLoss(
                        matchId,
                        OddsCategory.OVERSEAS,
                        automaticBet365Origin(),
                        homeOdds,
                        drawOdds,
                        awayOdds,
                        parsedOddsRow.apiOddsId(),
                        parsedOddsRow.publishedAt()
                ));
            } catch (RuntimeException exception) {
                logInvalidMarket(matchId, "승무패", parsedOddsRow, exception);
            }
        }

        return List.copyOf(histories);
    }

    private List<MatchOddsHistory> normalizeHandicap(
            Long matchId,
            SportCode sportCode,
            Integer matchingDirection,
            BetsEventOddsResponse response
    ) {
        if (!isSupportedMatchingDirection(matchingDirection)) {
            log.warn(
                    "BetsAPI 핸디캡 정규화 스킵 - 지원하지 않는 matching_dir. matchId:{}, matchingDir:{}",
                    matchId,
                    matchingDirection
            );
            return List.of();
        }

        List<ParsedOddsRow> parsedOddsRows = findParsedOddsRows(
                matchId,
                response,
                HANDICAP_MARKET_SUFFIX,
                ignored -> true
        );
        List<MatchOddsHistory> histories = new ArrayList<>();

        for (ParsedOddsRow parsedOddsRow : parsedOddsRows) {
            try {
                BetsEventOddsResponse.OddsRecord row = parsedOddsRow.row();
                BigDecimal homeOdds = parseDecimal(row.homeOdds(), "home_od");
                BigDecimal awayOdds = parseDecimal(row.awayOdds(), "away_od");
                BigDecimal handicapLine = parseDecimal(row.handicap(), "handicap");

                if (shouldSwapSides(sportCode, matchingDirection)) {
                    BigDecimal originalHomeOdds = homeOdds;
                    homeOdds = awayOdds;
                    awayOdds = originalHomeOdds;
                    handicapLine = handicapLine.negate();
                }

                histories.add(MatchOddsHistory.createHandicap(
                        matchId,
                        OddsCategory.OVERSEAS,
                        automaticBet365Origin(),
                        homeOdds,
                        handicapLine,
                        awayOdds,
                        parsedOddsRow.apiOddsId(),
                        parsedOddsRow.publishedAt()
                ));
            } catch (RuntimeException exception) {
                logInvalidMarket(matchId, "핸디캡", parsedOddsRow, exception);
            }
        }

        return List.copyOf(histories);
    }

    private List<MatchOddsHistory> normalizeUnderOver(Long matchId, BetsEventOddsResponse response) {
        List<ParsedOddsRow> parsedOddsRows = findParsedOddsRows(
                matchId,
                response,
                UNDER_OVER_MARKET_SUFFIX,
                this::hasSupportedUnderOverLine
        );
        List<MatchOddsHistory> histories = new ArrayList<>();

        for (ParsedOddsRow parsedOddsRow : parsedOddsRows) {
            try {
                BetsEventOddsResponse.OddsRecord row = parsedOddsRow.row();

                histories.add(MatchOddsHistory.createUnderOver(
                        matchId,
                        OddsCategory.OVERSEAS,
                        automaticBet365Origin(),
                        parseDecimal(row.underOdds(), "under_od"),
                        parseDecimal(row.handicap(), "handicap"),
                        parseDecimal(row.overOdds(), "over_od"),
                        parsedOddsRow.apiOddsId(),
                        parsedOddsRow.publishedAt()
                ));
            } catch (RuntimeException exception) {
                logInvalidMarket(matchId, "언더오버", parsedOddsRow, exception);
            }
        }

        return List.copyOf(histories);
    }

    private List<ParsedOddsRow> findParsedOddsRows(
            Long matchId,
            BetsEventOddsResponse response,
            String marketSuffix,
            Predicate<BetsEventOddsResponse.OddsRecord> marketPolicy
    ) {
        List<BetsEventOddsResponse.OddsRecord> marketRows = findMarketRows(
                matchId,
                response.results().odds(),
                marketSuffix
        );

        List<ParsedOddsRow> parsedOddsRows = marketRows.stream()
                .filter(Objects::nonNull)
                .filter(this::isPrematch)
                .filter(marketPolicy)
                .map(row -> toParsedOddsRow(matchId, marketSuffix, row))
                .flatMap(Optional::stream)
                .toList();

        return deduplicateParsedOddsRows(matchId, marketSuffix, parsedOddsRows).stream()
                .sorted(PARSED_ODDS_ROW_ORDER)
                .toList();
    }

    private List<ParsedOddsRow> deduplicateParsedOddsRows(
            Long matchId,
            String marketSuffix,
            List<ParsedOddsRow> parsedOddsRows
    ) {
        Map<String, ParsedOddsRow> parsedOddsRowByApiOddsId = new HashMap<>();
        Set<String> conflictingApiOddsIds = new HashSet<>();

        for (ParsedOddsRow parsedOddsRow : parsedOddsRows) {
            String apiOddsId = parsedOddsRow.apiOddsId();
            if (conflictingApiOddsIds.contains(apiOddsId)) {
                continue;
            }

            ParsedOddsRow existing = parsedOddsRowByApiOddsId.putIfAbsent(apiOddsId, parsedOddsRow);
            if (existing != null && !existing.row().equals(parsedOddsRow.row())) {
                parsedOddsRowByApiOddsId.remove(apiOddsId);
                conflictingApiOddsIds.add(apiOddsId);
                log.warn(
                        "BetsAPI 배당률 row 제외 - 동일 apiOddsId의 값이 서로 다릅니다. "
                                + "matchId:{}, marketSuffix:{}, apiOddsId:{}",
                        matchId,
                        marketSuffix,
                        apiOddsId
                );
            }
        }

        return List.copyOf(parsedOddsRowByApiOddsId.values());
    }

    private List<BetsEventOddsResponse.OddsRecord> findMarketRows(
            Long matchId,
            Map<String, List<BetsEventOddsResponse.OddsRecord>> oddsByMarket,
            String marketSuffix
    ) {
        List<Map.Entry<String, List<BetsEventOddsResponse.OddsRecord>>> matchedMarkets = oddsByMarket.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().endsWith(marketSuffix))
                .toList();

        if (matchedMarkets.isEmpty()) {
            return List.of();
        }

        if (matchedMarkets.size() > 1) {
            log.warn(
                    "BetsAPI 배당률 정규화 스킵 - 동일 마켓 키가 여러 개입니다. matchId:{}, marketSuffix:{}, keys:{}",
                    matchId,
                    marketSuffix,
                    matchedMarkets.stream().map(Map.Entry::getKey).toList()
            );
            return List.of();
        }

        return matchedMarkets.get(0).getValue();
    }

    private Optional<ParsedOddsRow> toParsedOddsRow(
            Long matchId,
            String marketSuffix,
            BetsEventOddsResponse.OddsRecord row
    ) {
        if (row.id() == null || row.id().isBlank()) {
            log.warn(
                    "BetsAPI 배당률 row 제외 - 외부 배당률 ID 누락. matchId:{}, marketSuffix:{}",
                    matchId,
                    marketSuffix
            );
            return Optional.empty();
        }

        try {
            long epochSecond = Long.parseLong(requireText(row.addTime(), "add_time"));
            return Optional.of(new ParsedOddsRow(
                    row,
                    row.id().trim(),
                    Instant.ofEpochSecond(epochSecond)
            ));
        } catch (IllegalArgumentException | DateTimeException exception) {
            log.warn(
                    "BetsAPI 배당률 row 제외 - 제공 시각이 올바르지 않습니다. matchId:{}, marketSuffix:{}, "
                            + "apiOddsId:{}, addTime:{}",
                    matchId,
                    marketSuffix,
                    row.id(),
                    row.addTime()
            );
            return Optional.empty();
        }
    }

    private boolean hasSupportedUnderOverLine(BetsEventOddsResponse.OddsRecord row) {
        try {
            UnderOverLine.from(parseDecimal(row.handicap(), "handicap"));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isPrematch(BetsEventOddsResponse.OddsRecord row) {
        return row.ss() == null || row.ss().isBlank();
    }

    private boolean shouldSwapSides(SportCode sportCode, int matchingDirection) {
        return switch (sportCode) {
            case FOOTBALL -> matchingDirection == MATCHING_DIRECTION_REVERSED;
            case BASEBALL -> matchingDirection == MATCHING_DIRECTION_SAME;
        };
    }

    private boolean isSupportedMatchingDirection(Integer matchingDirection) {
        return Objects.equals(matchingDirection, MATCHING_DIRECTION_SAME)
                || Objects.equals(matchingDirection, MATCHING_DIRECTION_REVERSED);
    }

    private Integer resolveMatchingDirection(BetsEventOddsResponse response) {
        BetsEventOddsResponse.Stats stats = response.results().stats();
        return stats == null ? null : stats.matchingDir();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        return new BigDecimal(requireText(value, fieldName));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값은 필수입니다.");
        }
        return value.trim();
    }

    private OddsOrigin automaticBet365Origin() {
        return OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
    }

    private void validateInput(Long matchId, SportCode sportCode, BetsEventOddsResponse response) {
        if (matchId == null) {
            throw new IllegalArgumentException("matchId는 필수입니다.");
        }
        if (sportCode == null) {
            throw new IllegalArgumentException("sportCode는 필수입니다.");
        }
        if (response == null
                || !Integer.valueOf(1).equals(response.success())
                || response.results() == null
                || response.results().odds() == null) {
            throw new IllegalArgumentException("BetsAPI 배당률 응답 구조가 올바르지 않습니다.");
        }
    }

    private void logInvalidMarket(
            Long matchId,
            String marketName,
            ParsedOddsRow parsedOddsRow,
            RuntimeException exception
    ) {
        log.warn(
                "BetsAPI {} 정규화 스킵 - 유효하지 않은 값입니다. matchId:{}, apiOddsId:{}, reason:{}",
                marketName,
                matchId,
                parsedOddsRow.apiOddsId(),
                exception.getMessage()
        );
    }

    private static int compareApiOddsIds(String first, String second) {
        try {
            return new BigInteger(first).compareTo(new BigInteger(second));
        } catch (NumberFormatException ignored) {
            return first.compareTo(second);
        }
    }

    private record ParsedOddsRow(
            BetsEventOddsResponse.OddsRecord row,
            String apiOddsId,
            Instant publishedAt
    ) {
    }
}
