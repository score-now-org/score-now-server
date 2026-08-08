package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FootballStats {
    private Integer yellowCards;   // 경고
    private Integer redCards;      // 퇴장
    private Integer shots;         // 슈팅
    private Integer shotsOnTarget; // 유효슈팅
    private Integer possession;    // 점유율 (%)
    private Integer offsides;      // 옵사이드
    private Integer fouls;         // 파울
    private Integer corners;       // 코너킥
    private Integer freeKicks;     // 프리킥

    public static FootballStats empty() {
        return FootballStats.builder().build();
    }
}
