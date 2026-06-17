package com.scorenow.scorenow_api.domain.player.dto.request;

import com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 선수 가용 상태 수정 요청")
public class AdminPlayerAvailabilityUpdateRequest {

    @NotNull(message = "스쿼드/결장자 상태는 필수입니다.")
    @Schema(description = "선수 가용 상태", example = "INJURED")
    private PlayerAvailabilityStatus availabilityStatus;
}
