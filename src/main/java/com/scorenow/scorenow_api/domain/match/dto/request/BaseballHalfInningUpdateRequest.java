package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseballHalfInningUpdateRequest {
    @NotNull
    private BaseballHalfInning halfInning;
}
