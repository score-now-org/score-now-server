package com.scorenow.scorenow_api.domain.match.dto.sse;

import static org.assertj.core.api.Assertions.assertThat;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BaseballDetailChangedPayloadTest {

    @Test
    void 말_점수의_합계를_홈팀_총점으로_초_점수의_합계를_원정팀_총점으로_변환한다() {
        BaseballInningScores inningScores = inningScores(Map.of(
                BaseballHalfInning.FIRST_INNING_TOP, 2,
                BaseballHalfInning.FIRST_INNING_BOTTOM, 1,
                BaseballHalfInning.SECOND_INNING_TOP, 1,
                BaseballHalfInning.SECOND_INNING_BOTTOM, 3));
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .inningScores(inningScores)
                .build();

        BaseballDetailChangedPayload payload = BaseballDetailChangedPayload.from(baseballDetail);

        assertThat(payload.getHomeScore()).isEqualTo(4);
        assertThat(payload.getAwayScore()).isEqualTo(3);
    }

    @Test
    void 이닝별_점수가_없으면_홈팀과_원정팀_총점도_null이다() {
        BaseballDetail baseballDetail = BaseballDetail.empty();

        BaseballDetailChangedPayload payload = BaseballDetailChangedPayload.from(baseballDetail);

        assertThat(payload.getHomeScore()).isNull();
        assertThat(payload.getAwayScore()).isNull();
    }

    private BaseballInningScores inningScores(Map<BaseballHalfInning, Integer> values) {
        EnumMap<BaseballHalfInning, Integer> scores = new EnumMap<>(BaseballHalfInning.class);
        scores.putAll(values);
        return BaseballInningScores.of(scores);
    }
}
