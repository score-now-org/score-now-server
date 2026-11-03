package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PrematchOddsCollectionTargetReaderTest {

    @InjectMocks
    private PrematchOddsCollectionTargetReader targetReader;

    @Mock
    private MatchRepository matchRepository;

    @Test
    void 축구와_야구의_BetsAPI_경기만_수집_대상으로_조회한다() {
        LocalDateTime startExclusive = LocalDateTime.of(2026, 11, 4, 10, 0);
        LocalDateTime endInclusive = LocalDateTime.of(2026, 11, 6, 9, 0);
        List<Long> targetIds = List.of(1L, 2L);

        given(matchRepository.findPrematchOddsCollectionTargetIds(
                BETS,
                NOT_STARTED,
                Set.of(SportCode.FOOTBALL, SportCode.BASEBALL),
                startExclusive,
                endInclusive
        )).willReturn(targetIds);

        List<Long> result = targetReader.findTargets(startExclusive, endInclusive);

        assertThat(result).containsExactly(1L, 2L);
        then(matchRepository).should().findPrematchOddsCollectionTargetIds(
                BETS,
                NOT_STARTED,
                Set.of(SportCode.FOOTBALL, SportCode.BASEBALL),
                startExclusive,
                endInclusive
        );
    }

    @Test
    void 종료_시각이_시작_시각보다_늦지_않으면_조회하지_않는다() {
        LocalDateTime standardTime = LocalDateTime.of(2026, 11, 4, 9, 0);

        assertThatThrownBy(() -> targetReader.findTargets(standardTime, standardTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배당률 수집 대상 조회 시간 범위가 올바르지 않습니다.");

        then(matchRepository).shouldHaveNoInteractions();
    }
}
