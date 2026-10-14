package com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InitialSportDetailProviderRegistryTest {

    private final InitialSportDetailProviderRegistry registry =
            new InitialSportDetailProviderRegistry(List.of(
                    new FootballInitialSportDetailProvider(),
                    new BaseballInitialSportDetailProvider()
            ));

    @Test
    void 축구_초기_상세정보를_생성한다() {
        assertThat(registry.create(SportCode.FOOTBALL))
                .isInstanceOf(FootballDetail.class);
    }

    @Test
    void 야구_초기_상세정보는_상세중계가_비활성화된_상태로_생성한다() {
        BaseballDetail detail = (BaseballDetail) registry.create(SportCode.BASEBALL);

        assertThat(detail.isDetailedBroadcastEnabled()).isFalse();
    }
}
