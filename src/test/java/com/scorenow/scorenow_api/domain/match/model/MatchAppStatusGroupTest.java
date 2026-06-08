package com.scorenow.scorenow_api.domain.match.model;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchAppStatusGroupTest {

    @Test
    void 앱_상태_그룹을_원천_경기_상태에서_찾는다() {
        // IN_PLAY GROUP
        assertThat(MatchAppStatusGroup.from(MatchStatus.IN_PLAY)).isEqualTo(MatchAppStatusGroup.IN_PLAY);
        assertThat(MatchAppStatusGroup.from(MatchStatus.INTERRUPTED)).isEqualTo(MatchAppStatusGroup.IN_PLAY);

        // SCHEDULED GROUP
        assertThat(MatchAppStatusGroup.from(MatchStatus.NOT_STARTED)).isEqualTo(MatchAppStatusGroup.SCHEDULED);

        // ENDED GROUP
        assertThat(MatchAppStatusGroup.from(MatchStatus.ENDED)).isEqualTo(MatchAppStatusGroup.ENDED);
        assertThat(MatchAppStatusGroup.from(MatchStatus.POSTPONED)).isEqualTo(MatchAppStatusGroup.ENDED);
        assertThat(MatchAppStatusGroup.from(MatchStatus.CANCELLED)).isEqualTo(MatchAppStatusGroup.ENDED);
        assertThat(MatchAppStatusGroup.from(MatchStatus.ABANDONED)).isEqualTo(MatchAppStatusGroup.ENDED);
    }

    @Test
    void 앱_조회_대상이_아닌_상태는_그룹이_없다() {
        assertThat(MatchAppStatusGroup.findBy(MatchStatus.WALKOVER)).isEmpty();
        assertThat(MatchAppStatusGroup.findBy(MatchStatus.TO_BE_FIXED)).isEmpty();
        assertThat(MatchAppStatusGroup.findBy(MatchStatus.RETIRED)).isEmpty();
        assertThat(MatchAppStatusGroup.findBy(MatchStatus.REMOVED)).isEmpty();
    }
}
