package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "외부 API 동기화 여부 수정 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueSyncEnabledUpdateRequest {

    @NotNull(message = "외부 API 동기화 활성화 여부는 필수입니다.")
    @JsonProperty("syncEnabled")
    @Schema(description = "외부 API 경기 동기화 활성화 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean syncEnabled;
}
