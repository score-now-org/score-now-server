package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record BaseballAppLiveSummaryResponse(
        boolean detailedBroadcastEnabled,
        List<BaseStateResponse> baseStates,
        Integer outs
) implements MatchAppSportLiveSummaryResponse {

    public static BaseballAppLiveSummaryResponse from(BaseballDetail baseballDetail) {
        BaseballCount count = baseballDetail.getCount();

        return new BaseballAppLiveSummaryResponse(
                baseballDetail.isDetailedBroadcastEnabled(),
                resolveBaseStates(baseballDetail.getBaseStates()),
                count == null ? null : count.getOuts()
        );
    }

    private static List<BaseStateResponse> resolveBaseStates(BaseballBaseStates baseStates) {
        if (baseStates == null) {
            return null;
        }

        Map<BaseballBase, BaseballBaseStates.BaseballBaseState> states = baseStates.getBaseStates();

        return Arrays.stream(BaseballBase.values())
                .map(base -> BaseStateResponse.from(base, states.get(base)))
                .toList();
    }

    public record BaseStateResponse(
            Integer baseNumber,
            boolean runnerOnBase
    ) {

        private static BaseStateResponse from(
                BaseballBase base,
                BaseballBaseStates.BaseballBaseState baseState) {

            return new BaseStateResponse(
                    base.getBaseNumber(),
                    baseState != null && baseState.isRunnerOnBase()
            );
        }
    }
}
