package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.baseball;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BaseballInplayStatusDisplayDetailResponse {
    private String halfInningCode;
    private Integer inningNumber;
    private String halfInning;

    public static BaseballInplayStatusDisplayDetailResponse from(BaseballHalfInning halfInning) {
        if (halfInning == null) {
            return BaseballInplayStatusDisplayDetailResponse.builder().build();
        }

        return BaseballInplayStatusDisplayDetailResponse.builder()
                .halfInningCode(halfInning.name())
                .inningNumber(halfInning.getInningNumber())
                .halfInning(halfInning.getHalf().name())
                .build();
    }
}
