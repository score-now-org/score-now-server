package com.scorenow.scorenow_api.domain.team.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 팀 관리 화면 선택 옵션 응답")
public class AdminTeamSearchOptionsResponse {

    @Schema(description = "팀 타입 목록")
    private List<TeamTypeOption> teamTypes;

    @Schema(description = "종목 목록")
    private List<SportOption> sports;

    @Getter
    @Builder
    @Schema(description = "팀 타입 옵션")
    public static class TeamTypeOption {
        @Schema(description = "팀 타입 코드", example = "CLUB")
        private String code;

        @Schema(description = "표시명", example = "클럽")
        private String name;
    }

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
