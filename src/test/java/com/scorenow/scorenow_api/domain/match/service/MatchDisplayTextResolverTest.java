package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchDisplayTextResolverTest {

    private final MatchDisplayTextResolver resolver = new MatchDisplayTextResolver();

    @Test
    void 전반_진행중이면_전반과_경과시간을_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.FIRST_HALF, 30, 12, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("전반 30");
        assertThat(displayElapsedMinutes).isEqualTo(30);
    }

    @Test
    void 후반_시작시_누적_45분을_후반_0분으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("후반 0");
        assertThat(displayElapsedMinutes).isEqualTo(0);
    }

    @Test
    void 전반이_종료되고_후반_시작전이면_전반_종료로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.SECOND_HALF, 45, 0, false, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);

        assertThat(displayText).isEqualTo("전반 종료");
    }

    @Test
    void 후반_진행중이면_누적시간을_후반_기준_시간으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.SECOND_HALF, 75, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("후반 30");
        assertThat(displayElapsedMinutes).isEqualTo(30);
    }

    @Test
    void 후반이_종료되고_연장전_시작전이면_후반_종료로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_FIRST_HALF, 90, 0, false, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);

        assertThat(displayText).isEqualTo("후반 종료");
    }

    @Test
    void 연장_전반_시작시_누적_90분을_연장_전반_0분으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_FIRST_HALF, 90, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("연장 전반 0");
        assertThat(displayElapsedMinutes).isEqualTo(0);
    }

    @Test
    void 연장_전반_진행중이면_누적시간을_연장_전반_기준_시간으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_FIRST_HALF, 103, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("연장 전반 13");
        assertThat(displayElapsedMinutes).isEqualTo(13);
    }

    @Test
    void 연장_전반이_종료되고_연장_후반_시작전이면_연장_전반_종료로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_SECOND_HALF, 105, 0, false, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);

        assertThat(displayText).isEqualTo("연장 전반 종료");
    }

    @Test
    void 연장_후반_시작시_누적_105분을_연장_후반_0분으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_SECOND_HALF, 105, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("연장 후반 0");
        assertThat(displayElapsedMinutes).isEqualTo(0);
    }

    @Test
    void 연장_후반_진행중이면_누적시간을_연장_후반_기준_시간으로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.EXTRA_SECOND_HALF, 118, 0, true, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("연장 후반 13");
        assertThat(displayElapsedMinutes).isEqualTo(13);
    }

    @Test
    void 승부차기_시작전이면_승부차기로_표시한다() {
        MatchDetailDocument.MatchClock matchClock = createMatchClock(MatchPeriod.PENALTY_SHOOTOUT, 120, 0, false, 0);

        String displayText = resolver.resolveInPlayDisplayText(matchClock);
        Integer displayElapsedMinutes = resolver.resolvePeriodElapsedMinutes(matchClock);

        assertThat(displayText).isEqualTo("승부차기");
        assertThat(displayElapsedMinutes).isNull();
    }

    @Test
    void 종료된_경기_결과를_표시문구로_변환한다() {
        assertThat(resolver.resolveEndedDisplayText(MatchResult.HOME_WIN)).isEqualTo("홈팀 승");
        assertThat(resolver.resolveEndedDisplayText(MatchResult.AWAY_WIN)).isEqualTo("홈팀 패");
        assertThat(resolver.resolveEndedDisplayText(MatchResult.DRAW)).isEqualTo("무승부");
    }

    private MatchDetailDocument.MatchClock createMatchClock(
            MatchPeriod period,
            Integer elapsedMinutes,
            Integer elapsedSeconds,
            Boolean running,
            Integer additionalMinutes
    ) {
        return MatchDetailDocument.MatchClock.builder()
                .period(period)
                .elapsedMinutes(elapsedMinutes)
                .elapsedSeconds(elapsedSeconds)
                .running(running)
                .additionalMinutes(additionalMinutes)
                .build();
    }
}
