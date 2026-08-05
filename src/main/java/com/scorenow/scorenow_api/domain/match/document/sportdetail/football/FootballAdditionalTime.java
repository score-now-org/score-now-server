package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

/**
 * [FootballClock 에 넣지 않고 별도로 분리한 이유]
 * FootballClock 의 경우에는 특정 시점의 스냅샷 정보를 저장한다.
 * 반면, FootballAdditionalTime 은 경기 중에 발생한 추가시간의 정보를 누적한다.
 * 즉, 성격 자체가 다르기 때문에 분리해서 저장하는 구조로 설계
 */
@Getter
@Builder
public class FootballAdditionalTime {
    private Integer firstHalf;          // 전반 추가시간
    private Integer secondHalf;         // 후반 추가시간
    private Integer extraFirstHalf;     // 연장 전반 추가시간
    private Integer extraSecondHalf;    // 연장 후반 추가시간
}
