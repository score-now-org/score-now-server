package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Getter
public class BaseballBaseStates {
    private final Map<BaseballBase, BaseballBaseState> baseStates;

    private BaseballBaseStates(Map<BaseballBase, BaseballBaseState> baseStates) {
        this.baseStates = Collections.unmodifiableMap(baseStates);
    }

    public static BaseballBaseStates initialize() {
        Map<BaseballBase, BaseballBaseState> baseStates = new EnumMap<>(BaseballBase.class);

        for (BaseballBase base : BaseballBase.values()) {
            baseStates.put(base, BaseballBaseState.empty());
        }

        return new BaseballBaseStates(baseStates);
    }

    public static BaseballBaseStates of(Map<BaseballBase, BaseballBaseState> baseStates) {
        if (baseStates == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "베이스 정보는 필수입니다.");
        }

        Map<BaseballBase, BaseballBaseState> copy = new EnumMap<>(BaseballBase.class);
        for (var entry : baseStates.entrySet()) {
            if (entry.getKey() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "베이스는 필수 입니다.");
            }

            BaseballBaseState baseState = entry.getValue();
            if (baseState != null && !baseState.isRunnerOnBase() && baseState.hasRunnerName()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "비어있는 베이스에 주자를 등록할 수 없습니다.");
            }

            copy.put(entry.getKey(), entry.getValue());
        }

        for (BaseballBase base : BaseballBase.values()) {
            copy.putIfAbsent(base, BaseballBaseState.empty());
        }

        return new BaseballBaseStates(copy);
    }

    @Getter
    public static class BaseballBaseState {
        private final boolean runnerOnBase;
        private final String runnerName;

        public BaseballBaseState(boolean runnerOnBase, String runnerName) {
            this.runnerOnBase = runnerOnBase;
            this.runnerName = runnerName;
        }

        public static BaseballBaseState empty() {
            return new BaseballBaseState(false, "");
        }

        public boolean hasRunnerName() {
            return runnerName != null && !runnerName.isBlank();
        }
    }
}
