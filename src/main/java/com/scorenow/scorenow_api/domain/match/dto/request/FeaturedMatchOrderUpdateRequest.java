package com.scorenow.scorenow_api.domain.match.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상단고정/핫매치 순서 변경 요청")
public class FeaturedMatchOrderUpdateRequest {

    @Schema(description = "순서를 변경할 노출 날짜. yyyyMMdd 형식입니다.", example = "20260723", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "노출 날짜는 필수입니다.")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate displayDate;

    @Schema(description = "순서를 변경할 타입", example = "HOT_MATCH", allowableValues = {"HOT_MATCH", "PINNED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상단고정/핫매치 타입은 필수입니다.")
    private FeaturedMatchType type;

    @Schema(description = "변경 후 전체 상단고정/핫매치 설정 ID 목록. 배열 순서가 displayOrder 1, 2, 3...이 됩니다.", example = "[5, 3, 1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "순서 변경 대상은 필수입니다.")
    private List<@NotNull Long> featuredMatchIds;
}
