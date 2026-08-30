package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Getter
@Builder(toBuilder = true)
@TypeAlias(FootballDetail.TYPE_ALIAS)
public class FootballDetail implements SportDetail {

    public static final String TYPE_ALIAS = "football-detail";

    /* 승부차기 */
    private FootballShootOutScore shootOutScore;

    /* 경기 시간 관련 */
    private FootballClock clock;
    private FootballAdditionalTime additionalTime;

    /* 경기 스텟 관련 */
    private FootballStats homeStats;
    private FootballStats awayStats;

    public static FootballDetail empty() {
        return FootballDetail.builder().build();
    }

    public FootballDetail withFootballClock(FootballClock newClock) {
        return this.toBuilder()
                .clock(newClock)
                .build();
    }

    public FootballDetail withShootOutScore(FootballShootOutScore newShootOutScore) {
        return this.toBuilder()
                .shootOutScore(newShootOutScore)
                .build();
    }

    public FootballDetail withAdditionalTime(FootballAdditionalTime newAdditionalTime) {
        return this.toBuilder()
                .additionalTime(newAdditionalTime)
                .build();
    }

    public FootballDetail withStats(FootballStats newHomeStats, FootballStats newAwayStats) {
        return this.toBuilder()
                .homeStats(newHomeStats)
                .awayStats(newAwayStats)
                .build();
    }
}
