package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsHistoryRepository;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class Bet365OddsPersistenceService {

    private static final OddsCategory CATEGORY = OddsCategory.OVERSEAS;
    private static final OddsRegistrationType REGISTRATION_TYPE = OddsRegistrationType.AUTOMATIC;
    private static final OddsSource SOURCE = OddsSource.BETS_API;
    private static final OddsBookmaker BOOKMAKER = OddsBookmaker.BET365;
    private static final OddsOrigin ORIGIN = OddsOrigin.automatic(SOURCE, BOOKMAKER);

    private final MatchOddsHistoryRepository matchOddsHistoryRepository;

    /**
     * 마켓타입별 최신 제공 시각 가운데 가장 오래된 시각을 다음 조회 기준으로 사용한다.
     * 아직 저장된 마켓이 없으면 since_time을 생략할 수 있도록 null을 반환한다.
     */
    @Transactional(readOnly = true)
    public Instant resolveSinceTime(Long matchId) {
        List<Instant> latestPublishedAtPerMarket = matchOddsHistoryRepository.findLatestPublishedAtPerMarket(
                matchId,
                CATEGORY,
                REGISTRATION_TYPE,
                SOURCE,
                BOOKMAKER,
                OddsMarketType.getAllTypes()
        );

        return latestPublishedAtPerMarket.stream()
                .min(Instant::compareTo)
                .orElse(null);
    }

    /**
     * 동일 경기ㆍ마켓ㆍapiOddsId가 없는 이력만 저장한다.
     * 동일 마켓의 apiOddsId가 이미 존재하면 값의 동일 여부와 관계없이 다시 저장하지 않는다.
     */
    @Transactional
    public int saveNewHistories(Long matchId, List<MatchOddsHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return 0;
        }

        validateHistories(matchId, histories);

        Set<String> apiOddsIds = histories.stream()
                .map(MatchOddsHistory::getApiOddsId)
                .collect(toUnmodifiableSet());

        List<MatchOddsHistory> existingHistories = matchOddsHistoryRepository.findAllByProviderOddsIds(
                matchId,
                CATEGORY,
                REGISTRATION_TYPE,
                SOURCE,
                BOOKMAKER,
                apiOddsIds
        );

        Set<ProviderOddsKey> existingKeys = existingHistories.stream()
                .map(ProviderOddsKey::from)
                .collect(toUnmodifiableSet());

        List<MatchOddsHistory> newHistories = filterNewHistories(histories, existingKeys);

        if (newHistories.isEmpty()) {
            return 0;
        }

        matchOddsHistoryRepository.saveAll(newHistories);
        return newHistories.size();
    }

    private List<MatchOddsHistory> filterNewHistories(
            List<MatchOddsHistory> histories,
            Set<ProviderOddsKey> existingKeys
    ) {
        List<MatchOddsHistory> newHistories = new ArrayList<>();
        Set<ProviderOddsKey> handledKeys = new HashSet<>();

        for (MatchOddsHistory history : histories) {
            ProviderOddsKey key = ProviderOddsKey.from(history);
            if (!handledKeys.add(key)) {
                log.warn(
                        "🟠BetsAPI 배당률 저장 스킵 - 한 응답에 동일한 provider ID가 중복되었습니다. "
                                + "matchId:{}, marketType:{}, apiOddsId:{}",
                        history.getMatchId(),
                        history.getMarketType(),
                        history.getApiOddsId()
                );
                continue;
            }

            if (!existingKeys.contains(key)) {
                newHistories.add(history);
            }
        }

        return List.copyOf(newHistories);
    }

    private void validateHistories(Long matchId, List<MatchOddsHistory> histories) {
        for (MatchOddsHistory history : histories) {
            boolean valid = Objects.equals(matchId, history.getMatchId())
                    && history.getCategory() == CATEGORY
                    && ORIGIN.equals(history.getOrigin())
                    && history.getApiOddsId() != null
                    && !history.getApiOddsId().isBlank();

            if (!valid) {
                throw new IllegalArgumentException("자동 Bet365 배당률 저장 대상이 올바르지 않습니다.");
            }
        }
    }

    private record ProviderOddsKey(OddsMarketType marketType, String apiOddsId) {

        private static ProviderOddsKey from(MatchOddsHistory history) {
            return new ProviderOddsKey(history.getMarketType(), history.getApiOddsId());
        }
    }
}
