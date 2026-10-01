package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseballBatterUpdateRequest {
    @NotNull
    private String batter;
}
