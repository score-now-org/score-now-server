package com.scorenow.scorenow_api.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignStadiumRequest {
    @Schema(description = "경기장 ID", example = "1")
    @NotNull(message = "경기장 ID는 필수입니다.")
    @Positive(message = "경기장 ID는 양수여야 합니다.")
    private Long stadiumId;
}
