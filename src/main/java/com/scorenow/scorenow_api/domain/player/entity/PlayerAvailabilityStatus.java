package com.scorenow.scorenow_api.domain.player.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * - 관리자 스쿼드/결장자 페이지 에서 사용되는 상태값
 * - 용도 자체가 팀에 소속된 선수의 상태를 표현하기 때문에
 * 내부 도메인에서는 '결장'의 의미 보다는 '현재 가용 가능한 상태'를 나타내는 방식으로 네이밍
 */

@Getter
@RequiredArgsConstructor
public enum PlayerAvailabilityStatus {
    AVAILABLE("표시 없음"),
    INJURED("부상"),
    SUSPENDED("징계"),
    EXCLUDED("로스터 제외"),
    REGISTERED("로스터 등록");

    private final String description;
}
