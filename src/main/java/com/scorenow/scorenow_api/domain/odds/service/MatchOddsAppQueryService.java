package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.dto.cursor.MatchOddsHistoryCursor;
import com.scorenow.scorenow_api.domain.odds.dto.response.*;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.*;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsDisplayPolicyRepository;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsHistoryRepository;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import com.scorenow.scorenow_api.domain.odds.value.OddsValue;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchOddsAppQueryService {
    private static final int MAX_HISTORY_SLICE_SIZE = 50;

    private final MatchOddsHistoryRepository matchOddsHistoryRepository;
    private final MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository;

    /**
     * [앱 전용] 각 경기별 '승무패' 해외 배당률 조회 (가장 최근 건 + 직전 건)
     */
    public Map<Long, MatchOddsCategoryResponse> getOverseasWinDrawLossOddsByMatchIds(final Set<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Map.of();
        }

        // matchIds 를 기반으로 각 경기별 현재 '해외'배당률 표기 정책을 확인한다.
        // 참고) 표기정책 경우의 수 : 국내+자동/국내+수동/해외+자동/해외+수동
        List<MatchOddsDisplayPolicy> policies = matchOddsDisplayPolicyRepository
                .findAllByMatchIdInAndCategory(matchIds, OddsCategory.OVERSEAS);

        // 표기정책 : 해외+자동
        Set<Long> automaticMatchIds = extractMatchIdsByDisplayPolicy(
                policies,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.AUTOMATIC
        );

        // 표기정책 : 해외+수동
        Set<Long> manualMatchIds = extractMatchIdsByDisplayPolicy(
                policies,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL
        );

        // 각 표기 정책 단위로, 경기별 '승무패' 최신 배당률 2건을 조회한다. (해외+자동/해외+수동)
        //  - 해외+승무패+자동 를 기준으로 각 경기별 최신 배당률 2건 조회
        List<MatchOddsHistory> automaticOddsHistories = findLatestTwoActiveHistories(
                automaticMatchIds,
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365)
        );

        //  - 해외+승무패+수동 를 기준으로 각 경기별 최신 배당률 2건 조회
        List<MatchOddsHistory> manualOddsHistories = findLatestTwoActiveHistories(
                manualMatchIds,
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsOrigin.manual()
        );

        // 경기별 배당률 정보 그룹화
        Map<Long, List<MatchOddsHistory>> historiesByMatchId = groupHistoriesByMatchId(
                automaticOddsHistories,
                manualOddsHistories
        );


        Map<Long, MatchOddsCategoryResponse> responsesByMatchId = new HashMap<>();

        for (var entry : historiesByMatchId.entrySet()) {
            Long matchId = entry.getKey();
            List<MatchOddsHistory> oddsHistories = entry.getValue();

            responsesByMatchId.put(matchId, toWinDrawLossResponse(OddsCategory.OVERSEAS, oddsHistories));
        }

        return responsesByMatchId;
    }


    /**
     * [앱 전용] 각 경기의 마켓타입 별 배당률 내역 조회
     */
    public MatchOddsHistorySliceResponse getMatchOddsHistorySlice(
            final Long matchId,
            final OddsCategory category,
            final OddsMarketType marketType,
            final String encodedCursor,
            final int size
    ) {
        int normalizedSize = normalizeHistorySliceSize(size);

        MatchOddsDisplayPolicy policy = matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, category)
                .orElseThrow(() -> new BusinessException(ErrorCode.ODDS_DISPLAY_POLICY_NOT_FOUND));

        MatchOddsHistoryCursor cursor = MatchOddsHistoryCursor.parse(
                encodedCursor,
                category,
                marketType,
                policy.getDisplayedRegistrationType()
        );

        OddsOrigin origin = resolveDisplayedOrigin(category, policy.getDisplayedRegistrationType());
        Instant cursorPublishedAt = cursor == null ? null : cursor.publishedAt();
        Long cursorHistoryId = cursor == null ? null : cursor.historyId();
        Pageable pageable = PageRequest.of(0, normalizedSize + 1);

        List<MatchOddsHistory> fetchedHistories = matchOddsHistoryRepository.findActiveHistorySlice(
                matchId,
                category,
                marketType,
                OddsStatus.ACTIVE,
                origin.getRegistrationType(),
                origin.getSource(),
                origin.getBookmaker(),
                cursorPublishedAt,
                cursorHistoryId,
                pageable
        );

        boolean hasNext = fetchedHistories.size() > normalizedSize;
        List<MatchOddsHistory> pageHistories = hasNext
                ? fetchedHistories.subList(0, normalizedSize)
                : fetchedHistories;

        MatchOddsHistory previousForLatest = cursor == null && fetchedHistories.size() > 1
                ? fetchedHistories.get(1)
                : null;
        List<OddsRowResponse> rows = createHistoryRows(pageHistories, previousForLatest);
        String nextCursor = createNextCursor(
                category,
                marketType,
                policy.getDisplayedRegistrationType(),
                pageHistories,
                hasNext
        );

        return new MatchOddsHistorySliceResponse(category, marketType, rows, nextCursor, hasNext);
    }

    /**
     * 요청 크기가 허용 범위인지 검증한다.
     */
    private int normalizeHistorySliceSize(int size) {
        if (size <= 0 || size > MAX_HISTORY_SLICE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        return size;
    }

    /**
     * 조회한 이력을 행 응답으로 변환하며 전체 최신 행에만 직전 이력을 적용한다.
     */
    private List<OddsRowResponse> createHistoryRows(
            List<MatchOddsHistory> histories,
            MatchOddsHistory previousForLatest
    ) {
        List<OddsRowResponse> rows = new ArrayList<>(histories.size());

        for (int index = 0; index < histories.size(); index++) {
            MatchOddsHistory currentHistory = histories.get(index);
            MatchOddsHistory previousHistory = index == 0 ? previousForLatest : null;

            rows.add(new OddsRowResponse(
                    currentHistory.getPublishedAt(),
                    createCellsByMarketType(currentHistory, previousHistory)
            ));
        }

        return rows;
    }

    /**
     * 마켓 타입에 맞는 위치와 종류의 배당률 셀을 생성한다.
     */
    private List<OddsCellResponse> createCellsByMarketType(
            MatchOddsHistory currentHistory,
            MatchOddsHistory previousHistory
    ) {
        return switch (currentHistory.getMarketType()) {
            case WIN_DRAW_LOSS -> createWinDrawLossCells(currentHistory, previousHistory);
            case HANDICAP -> createHandicapCells(currentHistory, previousHistory);
            case UNDER_OVER -> createUnderOverCells(currentHistory, previousHistory);
        };
    }

    /**
     * 다음 페이지가 있을 때 현재 응답의 마지막 이력을 다음 조회 커서로 변환한다.
     */
    private String createNextCursor(
            OddsCategory category,
            OddsMarketType marketType,
            OddsRegistrationType registrationType,
            List<MatchOddsHistory> histories,
            boolean hasNext
    ) {
        if (!hasNext || histories.isEmpty()) {
            return null;
        }

        MatchOddsHistory lastHistory = histories.get(histories.size() - 1);
        return new MatchOddsHistoryCursor(
                category,
                marketType,
                registrationType,
                lastHistory.getPublishedAt(),
                lastHistory.getId()
        ).encode();
    }

    /**
     * 표기 정책에서 카테고리와 등록 방식이 일치하는 경기 ID를 추출한다.
     */
    private Set<Long> extractMatchIdsByDisplayPolicy(
            List<MatchOddsDisplayPolicy> policies,
            OddsCategory category,
            OddsRegistrationType registrationType
    ) {
        return policies.stream()
                .filter(policy -> policy.getCategory() == category)
                .filter(policy -> policy.getDisplayedRegistrationType() == registrationType)
                .map(MatchOddsDisplayPolicy::getMatchId)
                .collect(Collectors.toSet());
    }

    /**
     * 지정한 조건에 해당하는 경기별 최신 활성 배당률을 최대 2건 조회한다.
     */
    private List<MatchOddsHistory> findLatestTwoActiveHistories(
            Set<Long> matchIds,
            OddsCategory category,
            OddsMarketType marketType,
            OddsOrigin origin
    ) {
        if (matchIds.isEmpty()) {
            return List.of();
        }

        return matchOddsHistoryRepository.findLatestTwoActivePerMatch(
                matchIds,
                category,
                marketType,
                origin
        );
    }

    /**
     * 자동 및 수동 배당률 이력을 합쳐 조회 순서를 유지한 채 경기 ID별로 그룹화한다.
     */
    private Map<Long, List<MatchOddsHistory>> groupHistoriesByMatchId(
            List<MatchOddsHistory> automaticHistories,
            List<MatchOddsHistory> manualHistories
    ) {
        Map<Long, List<MatchOddsHistory>> historiesByMatchId = new HashMap<>();

        List<MatchOddsHistory> mergedHistories = Stream.concat(automaticHistories.stream(), manualHistories.stream()).toList();

        for (MatchOddsHistory history : mergedHistories) {
            historiesByMatchId
                    .computeIfAbsent(history.getMatchId(), newKey -> new ArrayList<>())
                    .add(history);
        }

        return historiesByMatchId;
    }

    /**
     * 최신순 이력의 첫 번째와 두 번째 값을 현재·직전 승무패 마켓 응답으로 변환한다.
     */
    private MatchOddsCategoryResponse toWinDrawLossResponse(
            OddsCategory category,
            List<MatchOddsHistory> histories
    ) {
        MatchOddsHistory currentHistory = histories.get(0);
        MatchOddsHistory previousHistory = histories.size() > 1 ? histories.get(1) : null;

        OddsMarketSnapshotResponse market = new OddsMarketSnapshotResponse(
                OddsMarketType.WIN_DRAW_LOSS,
                currentHistory.getPublishedAt(),
                createWinDrawLossCells(currentHistory, previousHistory)
        );

        return MatchOddsCategoryResponse.of(category, List.of(market));
    }

    /**
     * 승무패 이력으로 홈·무승부·원정 배당률 셀을 생성한다.
     */
    private List<OddsCellResponse> createWinDrawLossCells(
            MatchOddsHistory currentHistory,
            MatchOddsHistory previousHistory
    ) {
        List<OddsCellResponse> cells = new ArrayList<>();

        cells.add(createOddsCell(
                OddsCellPosition.LEFT,
                OddsCellType.HOME,
                currentHistory.getHomeOdds(),
                previousHistory == null ? null : previousHistory.getHomeOdds()
        ));

        if (currentHistory.getDrawOdds() != null) {
            cells.add(createOddsCell(
                    OddsCellPosition.CENTER,
                    OddsCellType.DRAW,
                    currentHistory.getDrawOdds(),
                    previousHistory == null ? null : previousHistory.getDrawOdds()
            ));
        }

        cells.add(createOddsCell(
                OddsCellPosition.RIGHT,
                OddsCellType.AWAY,
                currentHistory.getAwayOdds(),
                previousHistory == null ? null : previousHistory.getAwayOdds()
        ));

        return cells;
    }

    /**
     * 핸디캡 이력으로 홈·기준점·원정 배당률 셀을 생성한다.
     */
    private List<OddsCellResponse> createHandicapCells(
            MatchOddsHistory currentHistory,
            MatchOddsHistory previousHistory
    ) {
        List<OddsCellResponse> cells = new ArrayList<>();

        cells.add(createOddsCell(
                OddsCellPosition.LEFT,
                OddsCellType.HOME,
                currentHistory.getHomeOdds(),
                previousHistory == null ? null : previousHistory.getHomeOdds()
        ));

        cells.add(createLineCell(
                OddsCellPosition.CENTER,
                OddsCellType.HANDICAP_LINE,
                currentHistory.getHandicapLine().value()
        ));

        cells.add(createOddsCell(
                OddsCellPosition.RIGHT,
                OddsCellType.AWAY,
                currentHistory.getAwayOdds(),
                previousHistory == null ? null : previousHistory.getAwayOdds()
        ));

        return cells;
    }

    /**
     * 언더오버 이력으로 언더·기준점·오버 배당률 셀을 생성한다.
     */
    private List<OddsCellResponse> createUnderOverCells(
            MatchOddsHistory currentHistory,
            MatchOddsHistory previousHistory
    ) {
        List<OddsCellResponse> cells = new ArrayList<>();

        cells.add(createOddsCell(
                OddsCellPosition.LEFT,
                OddsCellType.UNDER,
                currentHistory.getUnderOdds(),
                previousHistory == null ? null : previousHistory.getUnderOdds()
        ));

        cells.add(createLineCell(
                OddsCellPosition.CENTER,
                OddsCellType.UNDER_OVER_LINE,
                currentHistory.getUnderOverLine().value()
        ));

        cells.add(createOddsCell(
                OddsCellPosition.RIGHT,
                OddsCellType.OVER,
                currentHistory.getOverOdds(),
                previousHistory == null ? null : previousHistory.getOverOdds()
        ));

        return cells;
    }

    /**
     * 현재와 직전 배당률을 비교해 증감 정보가 포함된 배당률 셀을 생성한다.
     */
    private OddsCellResponse createOddsCell(
            OddsCellPosition position,
            OddsCellType type,
            OddsValue currentOdds,
            OddsValue previousOdds
    ) {
        return OddsCellResponse.odds(
                position,
                type,
                currentOdds.value(),
                valueOrNull(previousOdds)
        );
    }

    /**
     * 증감 계산을 하지 않는 마켓 기준점 셀을 생성한다.
     */
    private OddsCellResponse createLineCell(OddsCellPosition position, OddsCellType type, BigDecimal value) {
        return OddsCellResponse.line(position, type, value);
    }

    /**
     * 카테고리와 노출 방식에 맞는 배당률 출처를 결정한다.
     */
    private OddsOrigin resolveDisplayedOrigin(OddsCategory category, OddsRegistrationType registrationType) {
        if (registrationType == OddsRegistrationType.MANUAL) {
            return OddsOrigin.manual();
        }

        return switch (category) {
            case DOMESTIC -> OddsOrigin.automatic(OddsSource.BETMAN, OddsBookmaker.BETMAN);
            case OVERSEAS -> OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
        };
    }

    /**
     * 배당률 값 객체가 없으면 null을, 있으면 내부 값을 반환한다.
     */
    private BigDecimal valueOrNull(OddsValue odds) {
        return odds == null ? null : odds.value();
    }

}
