package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상단고정/핫매치 등록 요청")
public class FeaturedMatchCreateRequest {

    @Schema(description = "상단고정/핫매치로 등록할 경기 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "경기 ID는 필수입니다.")
    private Long matchId;

    @Schema(description = "등록 타입", example = "HOT_MATCH", allowableValues = {"HOT_MATCH", "PINNED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상단고정/핫매치 타입은 필수입니다.")
    private FeaturedMatchType type;
}
