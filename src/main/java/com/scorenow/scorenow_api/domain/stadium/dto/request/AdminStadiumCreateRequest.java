package com.scorenow.scorenow_api.domain.stadium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 경기장 생성 요청")
public class AdminStadiumCreateRequest {

    @Schema(description = "종목 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "종목 ID 는 필수입니다.")
    private Long sportId;

    @Schema(description = "경기장명", example = "Neo Quimica Arena", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "경기장 이름은 필수입니다.")
    private String name;

    @Schema(description = "도시명", example = "Sao Paulo")
    private String city;
}
