package com.scorenow.scorenow_api.domain.league.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "리그 시즌 순위 그룹 표시 설정 수정 요청")
public class LeagueSeasonStandingsGroupMappingsUpdateRequest {

    @NotEmpty(message = "순위 그룹 설정은 한 개 이상이어야 합니다.")
    @Valid
    private List<Group> groups;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Group {

        @NotBlank(message = "순위 그룹 key는 필수입니다.")
        @Schema(description = "외부 순위 그룹 식별 key", example = "groupname:western conference")
        private String groupKey;

        @NotBlank(message = "순위 그룹 표시명은 필수입니다.")
        @Schema(description = "앱에 노출할 순위 그룹명", example = "서부")
        private String displayName;

        @NotNull(message = "순위 그룹 표시 순서는 필수입니다.")
        @PositiveOrZero(message = "순위 그룹 표시 순서는 0 이상이어야 합니다.")
        @Schema(description = "순위 그룹 표시 순서. 0부터 시작합니다.", example = "0")
        private Integer displayOrder;
    }
}
