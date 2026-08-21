package com.scorenow.scorenow_api.domain.stadium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 경기장 수정 요청")
public class AdminStadiumUpdateRequest {

    @Schema(description = "경기장명. null이면 변경하지 않습니다.", example = "Neo Quimica Arena")
    @Size(max = 255, message = "경기장 이름은 255자를 초과할 수 없습니다.")
    private String name;

    @Schema(description = "도시명. null이면 변경하지 않습니다.", example = "서울")
    @Size(max = 255, message = "도시명은 255자를 초과할 수 없습니다.")
    private String city;
}
