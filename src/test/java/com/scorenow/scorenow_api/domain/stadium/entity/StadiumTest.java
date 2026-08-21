package com.scorenow.scorenow_api.domain.stadium.entity;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StadiumTest {

    @Test
    void 경기장_생성시_활성_상태이다() {
        Stadium stadium = Stadium.of("서울월드컵경기장", 1L, "서울", MANUAL);

        assertThat(stadium.isActive()).isTrue();
    }

    @Test
    void 경기장을_비활성화한다() {
        Stadium stadium = Stadium.of("서울월드컵경기장", 1L, "서울", MANUAL);

        stadium.deactivate();

        assertThat(stadium.isActive()).isFalse();
    }
}
