package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BaseballDetailTest {

    @Test
    void 공수_전환시_카운트와_모든_진루_현황을_초기화한다() {
        BaseballCount currentCount = new BaseballCount(3, 2, 2);
        BaseballBaseStates currentBaseStates = BaseballBaseStates.of(Map.of(
                BaseballBase.FIRST_BASE,
                new BaseballBaseStates.BaseballBaseState(true, "1루 주자"),
                BaseballBase.SECOND_BASE,
                new BaseballBaseStates.BaseballBaseState(true, "2루 주자"),
                BaseballBase.THIRD_BASE,
                new BaseballBaseStates.BaseballBaseState(true, "3루 주자")));
        BaseballDetail detail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.FIRST_INNING_TOP)
                .count(currentCount)
                .baseStates(currentBaseStates)
                .pitcher("투수")
                .batter("타자")
                .build();

        BaseballDetail result = detail.withCurrentHalfInning(BaseballHalfInning.FIRST_INNING_BOTTOM);

        assertThat(result.getCurrentHalfInning()).isEqualTo(BaseballHalfInning.FIRST_INNING_BOTTOM);
        assertThat(result.getCount().getBalls()).isZero();
        assertThat(result.getCount().getStrikes()).isZero();
        assertThat(result.getCount().getOuts()).isZero();
        for (BaseballBase base : BaseballBase.values()) {
            BaseballBaseStates.BaseballBaseState baseState = result.getBaseStates().getBaseState(base);
            assertThat(baseState.isRunnerOnBase()).isFalse();
            assertThat(baseState.getRunnerName()).isEmpty();
        }
        assertThat(result.getPitcher()).isEqualTo("투수");
        assertThat(result.getBatter()).isEqualTo("타자");
    }

    @Test
    void 같은_이닝으로_변경하면_카운트와_진루_현황을_유지한다() {
        BaseballCount currentCount = new BaseballCount(2, 1, 1);
        BaseballBaseStates currentBaseStates = BaseballBaseStates.of(Map.of(
                BaseballBase.FIRST_BASE,
                new BaseballBaseStates.BaseballBaseState(true, "1루 주자")));
        BaseballDetail detail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.FIRST_INNING_TOP)
                .count(currentCount)
                .baseStates(currentBaseStates)
                .build();

        BaseballDetail result = detail.withCurrentHalfInning(BaseballHalfInning.FIRST_INNING_TOP);

        assertThat(result).isSameAs(detail);
        assertThat(result.getCount()).isSameAs(currentCount);
        assertThat(result.getBaseStates()).isSameAs(currentBaseStates);
    }
}
