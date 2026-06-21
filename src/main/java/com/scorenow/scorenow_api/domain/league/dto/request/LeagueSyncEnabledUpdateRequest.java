package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueSyncEnabledUpdateRequest {

    @NotNull(message = "외부 API 동기화 활성화 여부는 필수입니다.")
    @JsonProperty("syncEnabled")
    private Boolean syncEnabled;
}
