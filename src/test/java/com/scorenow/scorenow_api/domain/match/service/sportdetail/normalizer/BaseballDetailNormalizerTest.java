package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.FIRST_INNING_BOTTOM;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.FIRST_INNING_TOP;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.TENTH_INNING_BOTTOM;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.TENTH_INNING_TOP;
import static org.assertj.core.api.Assertions.assertThat;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsBaseballViewResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BaseballDetailNormalizerTest {

    private final BaseballDetailNormalizer normalizer = new BaseballDetailNormalizer();

    @Test
    void 이닝의_away는_초_점수로_home은_말_점수로_변환한다() {
        BetsBaseballViewResponse.ViewResult viewResult = viewResult(Map.of(
                "1", score("1", "2")));

        BaseballDetail result = normalizer.normalize(viewResult);

        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_TOP)).isEqualTo(2);
        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_BOTTOM)).isEqualTo(1);
        assertThat(result.getCurrentHalfInning()).isEqualTo(FIRST_INNING_BOTTOM);
    }

    @Test
    void 아직_말_점수가_없으면_현재_이닝을_초로_판단한다() {
        BetsBaseballViewResponse.ViewResult viewResult = viewResult(Map.of(
                "1", score(null, "0")));

        BaseballDetail result = normalizer.normalize(viewResult);

        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_TOP)).isZero();
        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_BOTTOM)).isNull();
        assertThat(result.getCurrentHalfInning()).isEqualTo(FIRST_INNING_TOP);
    }

    @Test
    void 점수의_공백을_제거하고_숫자가_아닌_값은_null로_변환한다() {
        BetsBaseballViewResponse.ViewResult viewResult = viewResult(Map.of(
                "1", score(" 2 ", "not-a-number")));

        BaseballDetail result = normalizer.normalize(viewResult);

        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_TOP)).isNull();
        assertThat(result.getInningScores().getHalfInningScore(FIRST_INNING_BOTTOM)).isEqualTo(2);
    }

    @Test
    void 이닝별_점수가_없으면_이닝_정보도_생성하지_않는다() {
        BetsBaseballViewResponse.ViewResult viewResult = viewResult(null);

        BaseballDetail result = normalizer.normalize(viewResult);

        assertThat(result.getInningScores()).isNull();
        assertThat(result.getCurrentHalfInning()).isNull();
    }

    @Test
    void 연장_이닝_점수도_정규화하고_현재_이닝을_판단한다() {
        Map<String, BetsBaseballViewResponse.Score> scores = new LinkedHashMap<>();
        for (int inning = 1; inning <= 9; inning++) {
            scores.put(String.valueOf(inning), score("0", "0"));
        }
        scores.put("10", score(null, "1"));

        BaseballDetail result = normalizer.normalize(viewResult(scores));

        assertThat(result.getInningScores().getHalfInningScore(TENTH_INNING_TOP)).isEqualTo(1);
        assertThat(result.getInningScores().getHalfInningScore(TENTH_INNING_BOTTOM)).isNull();
        assertThat(result.getCurrentHalfInning()).isEqualTo(TENTH_INNING_TOP);
    }

    private BetsBaseballViewResponse.ViewResult viewResult(
            Map<String, BetsBaseballViewResponse.Score> scores) {
        BetsBaseballViewResponse.ViewResult viewResult = new BetsBaseballViewResponse.ViewResult();
        viewResult.setScores(scores);
        return viewResult;
    }

    private BetsBaseballViewResponse.Score score(String home, String away) {
        BetsBaseballViewResponse.Score score = new BetsBaseballViewResponse.Score();
        score.setHome(home);
        score.setAway(away);
        return score;
    }
}
