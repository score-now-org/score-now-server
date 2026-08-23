package com.scorenow.scorenow_api.domain.stadium.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 경기장 관리 화면 선택 옵션 응답")
public class AdminStadiumSearchOptionsResponse {

    @Schema(description = "종목 목록")
    private List<SportOption> sports;

    @Getter
    @Builder
    @Schema(description = "종목 옵션")
    public static class SportOption {
        @Schema(description = "종목 ID", example = "1")
        private Long id;

        @Schema(description = "종목 코드", example = "FOOTBALL")
        private String code;

        @Schema(description = "표시명", example = "축구")
        private String name;
    }
}
