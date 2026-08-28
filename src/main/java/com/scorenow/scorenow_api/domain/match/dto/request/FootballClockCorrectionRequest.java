package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *  방법 1 : phase, 분, 초 모두 받는다.
 *  이거는 좀 그럴듯. 왜냐면 phase 까지 받아버리면 기존 phase 변경 기능이 좀 무의미해짐.
 *  근데 또 얘는 phase 변경하고 지금 몇분 몇초인지까지 한번에 수정하니까 또 괜찮을 것 같기도 하고
 *  뒤늦게 경기 추가하고 후반 12분 30초로 세팅해야 하는 상황이면 사용하기 좋지 않나?
 *  이렇게 안하면 후반 시작 버튼으로 phase 변경하고, 12분 30초 입력하고 보정해야 함 (2단계)
 *
 *  방법 2 : 분, 초 만 받는다.
 *  비교적 단순함.
 *
 *  참고로 이벤트는 발행하지 않는다.
 *  1. 기존 clock 관련 이벤트 발행 시,
 *     시간이 변경되면 이벤트 발행한다 라는 조건을 걸게되면 외부 데이터 동기화 시 항상 이벤트를 발행하게 된다.
 *     (물론 이거는 구조를 바꾸면 가능하긴 함.)
 *  2. 지금 하진 말자. 시간 보정 시 시간 보정 이벤트를 발행하는 것도 좋지만
 *     프론트엔드 입장에서는 이벤트 처리 스펙이 하나 더 늘게 되고, (어려운건아니지만)
 *     갑자기 이벤트가 와서 바꿔버리는 것보단
 *     그냥 사용자가 창 이동하고 왔는데 시간이 바뀌어 있는게 더 사용자 경험상 어색하지 않을듯?
 *     예를 들어, 후반 10분 였다가 시간 보정으로 후반 33분으로 바꿨는데, 유저가 보고 있다가 갑자기 바뀌면 뭐지 싶을듯
 *     그래서 그냥 두다가 창 이동하고 다시 보여주게 될 때, 그때 보정된 시간이 보여지면 어색함이 덜 할수도?
 */
@Getter
@NoArgsConstructor
public class FootballClockCorrectionRequest {

    @NotNull @Min(0)
    private Integer elapsedMinutes;

    @NotNull @Min(0) @Max(59)
    private Integer elapsedSeconds;
}
