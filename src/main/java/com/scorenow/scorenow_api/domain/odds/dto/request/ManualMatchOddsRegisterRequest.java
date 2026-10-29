package com.scorenow.scorenow_api.domain.odds.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ManualMatchOddsRegisterRequest(
        @Valid WinDrawLoss winDrawLoss,
        @Valid Handicap handicap,
        @Valid UnderOver underOver
) {

    public record WinDrawLoss(
            @NotNull(message = "홈승 배당률은 필수입니다.")
            @Positive(message = "홈승 배당률은 0보다 커야 합니다.")
            BigDecimal homeOdds,

            // 야구의 승패 마켓은 2-way이므로 null을 허용한다.
            @Positive(message = "무승부 배당률은 0보다 커야 합니다.")
            BigDecimal drawOdds,

            @NotNull(message = "원정승 배당률은 필수입니다.")
            @Positive(message = "원정승 배당률은 0보다 커야 합니다.")
            BigDecimal awayOdds
    ) {
    }

    public record Handicap(
            @NotNull(message = "홈 핸디캡 배당률은 필수입니다.")
            @Positive(message = "홈 핸디캡 배당률은 0보다 커야 합니다.")
            BigDecimal homeOdds,

            @NotNull(message = "핸디캡 기준값은 필수입니다.")
            BigDecimal handicapLine,

            @NotNull(message = "원정 핸디캡 배당률은 필수입니다.")
            @Positive(message = "원정 핸디캡 배당률은 0보다 커야 합니다.")
            BigDecimal awayOdds
    ) {
    }

    public record UnderOver(
            @NotNull(message = "언더 배당률은 필수입니다.")
            @Positive(message = "언더 배당률은 0보다 커야 합니다.")
            BigDecimal underOdds,

            @NotNull(message = "언더/오버 기준값은 필수입니다.")
            @Positive(message = "언더/오버 기준값은 0보다 커야 합니다.")
            BigDecimal underOverLine,

            @NotNull(message = "오버 배당률은 필수입니다.")
            @Positive(message = "오버 배당률은 0보다 커야 합니다.")
            BigDecimal overOdds
    ) {
    }
}
