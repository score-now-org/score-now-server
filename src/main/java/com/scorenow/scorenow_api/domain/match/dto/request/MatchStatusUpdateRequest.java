package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchStatusUpdateRequest {
    @NotNull
    @Schema(
            description = "변경할 경기 상태",
            example = "IN_PLAY",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
                    "NOT_STARTED", "IN_PLAY", "TO_BE_FIXED", "ENDED", "POSTPONED",
                    "CANCELLED", "WALKOVER", "INTERRUPTED", "ABANDONED", "RETIRED", "REMOVED"
            }
    )
    private MatchStatus status;
}
