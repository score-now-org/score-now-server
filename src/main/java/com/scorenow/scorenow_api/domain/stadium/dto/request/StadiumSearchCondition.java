package com.scorenow.scorenow_api.domain.stadium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 경기장 검색 조건")
public class StadiumSearchCondition {

    @Schema(description = "경기장 ID", example = "1")
    private Long stadiumId;

    @Schema(description = "경기장명 검색어", example = "Neo Quimica")
    private String name;
}
