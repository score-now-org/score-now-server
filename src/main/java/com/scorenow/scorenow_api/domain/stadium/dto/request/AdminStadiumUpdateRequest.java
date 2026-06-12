package com.scorenow.scorenow_api.domain.stadium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 경기장 수정 요청")
public class AdminStadiumUpdateRequest {

    @Schema(description = "경기장명. null이면 변경하지 않습니다.", example = "Neo Quimica Arena")
    private String name;
}
