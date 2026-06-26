package com.scorenow.scorenow_api.domain.match.dto.sse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStatusChangedPayload {
    private final String statusCode;
    private final String statusName;

    // TODO: 이거에 대해서 고민필요
    //       경기전 -> 경기중 으로 변경되었을 때, 문구를 바로 반영해야하기 때문
    //       (기존 api 응답의 경우 displayText 를 반환해줌) => 기존 api 의 응답으로 전반13 과 같이 시간값을 반환하게 될텐데,
    //       그 시점에서 시간을 프론트에서 직접 돌려야 하기 때문에 이 부분도 고민이 필요함.
    private final String displayText;
}
