package com.scorenow.scorenow_api.domain.odds.dto.request;

import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import jakarta.validation.constraints.NotNull;

public record OddsDisplayModeUpdateRequest(
        @NotNull(message = "배당률 노출 방식은 필수입니다.")
        OddsRegistrationType displayMode
) {
}
