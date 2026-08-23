package com.scorenow.scorenow_api.domain.match.document.sportdetail;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import java.util.Arrays;

/**
 * MatchDetailDocument 가 어떤 종목의 데이터인지 명시해주는 구분자
 * <p>
 * TODO: Bets sportId 직접 매핑은 임시 경로로만 사용한다. 추후 SportExternalMapping 으로 확인된
 *       Match.sportId 와 Sport.code 기반 SportDetailTypeResolver 를 통해 결정하도록 전환한다.
 */
public enum SportDetailType {
    FOOTBALL(1L), BASEBALL(2L);

    private final Long sportId;

    SportDetailType(Long sportId) {
        this.sportId = sportId;
    }

    public static SportDetailType fromSportId(Long sportId) {
        return Arrays.stream(values())
                .filter(type -> type.sportId.equals(sportId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "지원하지 않는 종목입니다. sportId=" + sportId));
    }
}
