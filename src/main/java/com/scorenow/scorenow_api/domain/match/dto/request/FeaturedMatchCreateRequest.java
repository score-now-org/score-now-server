package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상단고정/핫매치 등록 요청")
public class FeaturedMatchCreateRequest {

    @Schema(description = "상단고정/핫매치로 등록할 경기 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "등록할 경기 ID는 필수입니다.")
    @Size(max = 100, message = "한 번에 최대 100경기까지 등록할 수 있습니다.")
    private List<@NotNull Long> matchIds;

    @Schema(description = "등록 타입", example = "HOT_MATCH", allowableValues = {"HOT_MATCH", "PINNED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상단고정/핫매치 타입은 필수입니다.")
    private FeaturedMatchType type;
}