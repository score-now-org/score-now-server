package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BaseballInningScoresTest {

    @Test
    void 새_응답의_null은_기존_이닝_점수를_보존하고_0은_정상적으로_반영한다() {
        BaseballInningScores current = BaseballInningScores.of(Map.of(
                BaseballHalfInning.TENTH_INNING_TOP, 1,
                BaseballHalfInning.TENTH_INNING_BOTTOM, 2
        ));
        BaseballInningScores incoming = BaseballInningScores.of(Map.of(
                BaseballHalfInning.TENTH_INNING_TOP, 0
        ));

        BaseballInningScores merged = current.mergeProvided(incoming);

        assertThat(merged.getHalfInningScore(BaseballHalfInning.TENTH_INNING_TOP)).isZero();
        assertThat(merged.getHalfInningScore(BaseballHalfInning.TENTH_INNING_BOTTOM)).isEqualTo(2);
    }
}
