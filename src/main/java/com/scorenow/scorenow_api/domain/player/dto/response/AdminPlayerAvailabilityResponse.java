package com.scorenow.scorenow_api.domain.player.dto.response;

import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.team.entity.Team;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 선수 가용 상태 응답")
public class  AdminPlayerAvailabilityResponse {

    @Schema(description = "선수-팀 상세 ID", example = "1")
    private Long playerTeamDetailId;

    @Schema(description = "선수 ID", example = "1")
    private Long playerId;

    @Schema(description = "선수 한글명", example = "손흥민")
    private String playerKName;

    @Schema(description = "선수 영문명", example = "Son Heung-min")
    private String playerEName;

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "팀 한글명", example = "LAFC")
    private String teamKName;

    @Schema(description = "등번호", example = "7")
    private String shirtNumber;

    @Schema(description = "선수 가용 상태", example = "AVAILABLE")
    private PlayerAvailabilityStatus availabilityStatus;

    @Schema(description = "선수 가용 상태 설명", example = "표시 없음")
    private String availabilityStatusDescription;

    public static AdminPlayerAvailabilityResponse from(PlayerTeamDetail detail) {
        Player player = detail.getPlayer();
        Team team = detail.getTeam();

        return AdminPlayerAvailabilityResponse.builder()
                .playerTeamDetailId(detail.getId())
                .playerId(player != null ? player.getId() : null)
                .playerKName(player != null ? player.getKName() : null)
                .playerEName(player != null ? player.getEName() : null)
                .teamId(team != null ? team.getId() : null)
                .teamKName(team != null ? team.getKName() : null)
                .shirtNumber(detail.getShirtNumber())
                .availabilityStatus(detail.getAvailabilityStatus())
                .availabilityStatusDescription(
                        detail.getAvailabilityStatus() != null
                                ? detail.getAvailabilityStatus().getDescription()
                                : null
                )
                .build();
    }
}
