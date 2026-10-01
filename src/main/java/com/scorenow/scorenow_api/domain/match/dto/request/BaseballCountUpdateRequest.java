package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseballCountUpdateRequest {
    @NotNull
    @Min(0)
    @Max(3)
    private Integer balls;

    @NotNull
    @Min(0)
    @Max(2)
    private Integer strikes;

    @NotNull
    @Min(0)
    @Max(2)
    private Integer outs;

    public BaseballCount toBaseballCount() {
        return new BaseballCount(balls, strikes, outs);
    }
}
