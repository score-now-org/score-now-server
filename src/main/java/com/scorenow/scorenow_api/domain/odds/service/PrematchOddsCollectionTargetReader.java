package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrematchOddsCollectionTargetReader {

    private static final Set<SportCode> SUPPORTED_SPORTS = Set.of(
            SportCode.FOOTBALL,
            SportCode.BASEBALL
    );

    private final MatchRepository matchRepository;

    public List<Long> findTargets(LocalDateTime startExclusive, LocalDateTime endInclusive) {
        validateRange(startExclusive, endInclusive);

        return matchRepository.findPrematchOddsCollectionTargetIds(
                BETS,
                NOT_STARTED,
                SUPPORTED_SPORTS,
                startExclusive,
                endInclusive
        );
    }

    private void validateRange(LocalDateTime startExclusive, LocalDateTime endInclusive) {
        if (startExclusive == null || endInclusive == null || !endInclusive.isAfter(startExclusive)) {
            throw new IllegalArgumentException("배당률 수집 대상 조회 시간 범위가 올바르지 않습니다.");
        }
    }
}
