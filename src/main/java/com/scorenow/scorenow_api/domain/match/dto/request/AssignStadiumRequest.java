package com.scorenow.scorenow_api.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignStadiumRequest {
    @Schema(description = "임시 경기장명", example = "수원 월드컵 경기장")
    @NotNull(message = "경기장 ID는 필수입니다.")
    private Long stadiumId;
}
