package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchCreateRequest {

    @NotNull(message = "종목 ID는 필수입니다.")
    private Long sportId;

    @NotNull(message = "리그 ID는 필수입니다.")
    private Long leagueId;

    @NotNull(message = "홈팀 ID는 필수입니다.")
    private Long homeId;

    @NotNull(message = "원정팀 ID는 필수입니다.")
    private Long awayId;

    @NotNull(message = "경기 시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
}
