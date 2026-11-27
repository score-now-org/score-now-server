package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import com.scorenow.scorenow_api.domain.match.service.MatchEventResolver;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsFootballViewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FootballDetailNormalizerTest {

    private final FootballDetailNormalizer normalizer = new FootballDetailNormalizer(new MatchEventResolver());

    @Test
    void 홈과_원정의_유효_슈팅과_빗나간_슈팅을_팀별로_합산한다() {
        FootballDetail detail = normalize(List.of("7", "2"), List.of("5", "3"));

        assertThat(detail.getHomeStats().getShots()).isEqualTo(12);
        assertThat(detail.getHomeStats().getShotsOnTarget()).isEqualTo(5);
        assertThat(detail.getAwayStats().getShots()).isEqualTo(5);
        assertThat(detail.getAwayStats().getShotsOnTarget()).isEqualTo(3);
    }

    @Test
    void 원정_입력이_누락되어도_홈의_명시적인_0을_저장한다() {
        FootballDetail detail = normalize(List.of("0"), List.of("0", "4"));

        assertThat(detail.getHomeStats().getShots()).isZero();
        assertThat(detail.getHomeStats().getShotsOnTarget()).isZero();
        assertThat(detail.getAwayStats().getShots()).isNull();
        assertThat(detail.getAwayStats().getShotsOnTarget()).isEqualTo(4);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-1", "2.5", "오류", "2147483647", "2147483648"})
    void 빗나간_슈팅이_없거나_유효하지_않거나_합이_범위를_초과하면_전체_슈팅을_저장하지_않는다(String offTarget) {
        FootballDetail detail = normalize(Arrays.asList(offTarget, "2"), List.of("4", "3"));

        assertThat(detail.getHomeStats().getShots()).isNull();
        assertThat(detail.getHomeStats().getShotsOnTarget()).isEqualTo(4);
        assertThat(detail.getAwayStats().getShots()).isEqualTo(5);
        assertThat(detail.getAwayStats().getShotsOnTarget()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-1", "2.5", "오류", "2147483648"})
    void 유효_슈팅을_확인할_수_없으면_두_슈팅_항목_모두_null이다(String onTarget) {
        FootballDetail detail = normalize(List.of("7", "2"), Arrays.asList("5", onTarget));

        assertThat(detail.getHomeStats().getShots()).isEqualTo(12);
        assertThat(detail.getAwayStats().getShots()).isNull();
        assertThat(detail.getAwayStats().getShotsOnTarget()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "2, 95, 1, EXTRA_FIRST_HALF",
            "3, 110, 1, EXTRA_SECOND_HALF",
            "3, 120, 0, FULL_TIME",
            "2, 90, 0, EXTRA_TIME_WAITING",
            "4, 120, 0, PENALTY_SHOOTOUT"
    })
    void 타이머의_연장과_승부차기_구간을_변환한다(
            int period, int minute, String running, FootballPhase phase) {
        BetsFootballViewResponse.Timer timer = new BetsFootballViewResponse.Timer();
        timer.setMd(period);
        timer.setTm(minute);
        timer.setTt(running);
        BetsFootballViewResponse.ViewResult result = new BetsFootballViewResponse.ViewResult();
        result.setTimer(timer);

        FootballDetail detail = normalizer.normalize(result);

        assertThat(detail.getClock().getPhase()).isEqualTo(phase);
    }

    private FootballDetail normalize(List<String> offTarget, List<String> onTarget) {
        BetsFootballViewResponse.Stats stats = new BetsFootballViewResponse.Stats();
        stats.setOffTarget(offTarget);
        stats.setOnTarget(onTarget);
        BetsFootballViewResponse.ViewResult result = new BetsFootballViewResponse.ViewResult();
        result.setStats(stats);
        return normalizer.normalize(result);
    }
}
