package com.scorenow.scorenow_api.domain.league.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "관리자 리그 시즌 순위 그룹 표시 설정 응답")
public class AdminLeagueSeasonStandingsGroupMappingsResponse {

    @Schema(description = "리그 시즌 ID", example = "10")
    private Long leagueSeasonId;

    private List<Group> groups;

    @Getter
    @Builder
    public static class Group {

        @Schema(description = "외부 순위 그룹 식별 key", example = "groupname:western conference")
        private String groupKey;

        @Schema(description = "외부 API table name", example = "MLS 2026, Western Conference")
        private String externalName;

        @Schema(description = "외부 API groupname", example = "Western Conference")
        private String externalGroupName;

        @Schema(description = "앱에 노출할 순위 그룹명", example = "서부")
        private String displayName;

        @Schema(description = "순위 그룹 표시 순서", example = "0")
        private Integer displayOrder;

        @Schema(description = "앱 경기 목록에서 해당 순위 그룹 노출 여부", example = "true")
        private boolean visibleInMatchList;
    }
}
