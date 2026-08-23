package com.scorenow.scorenow_api.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignTemporaryStadiumRequest {
    @Schema(description = "임시 경기장명", example = "수원 월드컵 경기장")
    @NotBlank(message = "임시 경기장명은 필수입니다.")
    @Size(max = 255, message = "임시 경기장명은 255자를 초과할 수 없습니다.")
    private String stadiumName;

    @Schema(description = "도시명", example = "수원")
    @Size(max = 255, message = "도시명은 255자를 초과할 수 없습니다.")
    private String city;
}
