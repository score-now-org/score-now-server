package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "앱 경기 목록 및 주요 경기 조회 응답")
public class MatchAppResponse {

    @Schema(description = "조회 기준 날짜", example = "2026-06-05")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "상단고정 및 핫매치 경기 목록")
    private MatchAppFeaturedSectionResponse featuredMatches;

    @Schema(description = "리그별 경기 목록. v1 경기 목록과 동일한 구조입니다.")
    private List<MatchAppLeagueGroupResponse> leagueMatches;
}
