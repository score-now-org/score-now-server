package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

/**
 * TODO
 * 현재 투수 정보와 현재 타자 정보는 실제 해당 팀에 소속된 선수의 정보가 들어가야 한다.
 * 아직 해당 부분은 개발이 안되어 있으므로, 일단 현재 단계에서는 단순 문자열로 세팅하며
 * 추후 해당 부분이 개발 단계에 진입할 때, 어떻게 할지 고려한다.
 */

@Getter
@Builder(toBuilder = true)
@TypeAlias(BaseballDetail.TYPE_ALIAS)
public class BaseballDetail implements SportDetail {

    public static final String TYPE_ALIAS = "baseball-detail";

    /* 현재 이닝 */
    private BaseballHalfInning currentHalfInning;

    /* 이닝별 점수 */
    private BaseballInningScores inningScores;

    /* 주자, 투수정보, 볼/아웃 카운트, HEB 정보 제공 ON OFF */
    private boolean detailedBroadcastEnabled;

    /* 현재 투수 정보 */
    private String pitcher;

    /* 현재 타자 정보 */
    private String batter;

    /* 베이스 별 정보 */
    private BaseballBaseStates baseStates;

    /* 볼/아웃 카운트 */
    private BaseballCount count;

    /* H E B */
    private BaseballStats homeStats;
    private BaseballStats awayStats;

    public static BaseballDetail empty() {
        return BaseballDetail.builder().build();
    }

    public BaseballDetail withInningScores(BaseballInningScores newInningScores) {
        return this.toBuilder()
                .inningScores(newInningScores)
                .build();
    }

    public BaseballDetail withCurrentHalfInning(BaseballHalfInning halfInning) {
        if (currentHalfInning == halfInning) {
            return this;
        }

        return this.toBuilder()
                .currentHalfInning(halfInning)
                .count(BaseballCount.empty())   // 이닝 및 공수 전환 시, 볼/아웃 카운트 초기화
                .build();
    }

    public BaseballDetail withStats(BaseballStats homeStats, BaseballStats awayStats) {
        return this.toBuilder()
                .homeStats(homeStats)
                .awayStats(awayStats)
                .build();
    }

    public BaseballDetail withCount(BaseballCount count) {
        return this.toBuilder()
                .count(count)
                .build();
    }

    public BaseballDetail withDetailedBroadcastEnabled(boolean isEnabled) {
        return this.toBuilder()
                .detailedBroadcastEnabled(isEnabled)
                .build();
    }

    public BaseballDetail withBaseStates(BaseballBaseStates baseStates) {
        return this.toBuilder()
                .baseStates(baseStates)
                .build();
    }

    public BaseballDetail withPitcher(String pitcher) {
        return this.toBuilder()
                .pitcher(pitcher)
                .build();
    }

    public BaseballDetail withBatter(String batter) {
        return this.toBuilder()
                .batter(batter)
                .build();
    }

    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }
}
