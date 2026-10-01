package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates.BaseballBaseState;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 베이스 상태 전체 교체 요청. 생략한 베이스는 비우며, 빈 목록은 모든 베이스를 비운다.
 */
@Getter
@NoArgsConstructor
public class BaseballBaseStatesUpdateRequest {
    @NotNull
    @Size(max = 3)
    private List<@NotNull @Valid BaseState> baseStates;

    @Getter
    @NoArgsConstructor
    public static class BaseState {
        @NotNull
        @Min(1)
        @Max(3)
        private Integer baseNumber;

        @NotNull
        private Boolean runnerOnBase;

        private String runnerName;
    }

    public BaseballBaseStates toBaseballBaseStates() {
        Map<BaseballBase, BaseballBaseState> states = new EnumMap<>(BaseballBase.class);

        for (BaseState state : baseStates) {
            BaseballBase base = resolveBase(state.getBaseNumber());
            // 중복 체크
            if (states.containsKey(base)) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "중복되는 베이스 정보입니다. baseNumber=" + state.getBaseNumber());
            }

            // 진루하지 않았는데, 주자명이 존재하는지 확인
            if (!Objects.equals(state.getRunnerOnBase(), Boolean.TRUE) && state.getRunnerName() != null && !state.getRunnerName().isBlank()) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "비어있는 베이스에 주자를 등록할 수 없습니다. baseNumber=" + state.getBaseNumber());
            }

            states.put(base, new BaseballBaseState(
                    state.getRunnerOnBase() != null && state.getRunnerOnBase(),
                    state.getRunnerName())
            );
        }

        return BaseballBaseStates.of(states);
    }

    private BaseballBase resolveBase(int baseNumber) {
        for (BaseballBase base : BaseballBase.values()) {
            if (base.getBaseNumber() == baseNumber) {
                return base;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 베이스 번호입니다.");
    }
}
