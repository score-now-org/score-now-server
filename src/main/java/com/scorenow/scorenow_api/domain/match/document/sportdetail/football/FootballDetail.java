package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Getter
@Builder
@TypeAlias("football-detail")
public class FootballDetail implements SportDetail {

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
}
