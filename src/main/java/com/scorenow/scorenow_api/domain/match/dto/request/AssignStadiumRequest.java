package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignStadiumRequest {
    @NotNull(message = "경기장 ID는 필수입니다.")
    private Long stadiumId;
}
