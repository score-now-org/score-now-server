package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Schema(description = "상단고정/핫매치 등록 경기 응답")
public class FeaturedMatchResponse {

    @Schema(description = "상단고정/핫매치 설정 ID", example = "1")
    private final Long featuredMatchId;

    @Schema(description = "경기 ID", example = "1001")
    private final Long matchId;

    @Schema(description = "리그명", example = "프리미어리그")
    private final String leagueName;

    @Schema(description = "홈팀명", example = "토트넘")
    private final String homeTeamName;

    @Schema(description = "원정팀명", example = "아스널")
    private final String awayTeamName;

    @Schema(description = "경기 시작 시간. KST 기준 HH:mm 형식입니다.", example = "20:30")
    private final String startAt;

    @Schema(description = "등록 타입", example = "HOT_MATCH", allowableValues = {"HOT_MATCH", "PINNED"})
    private final String type;

    @Schema(description = "타입 내 노출 순서. 1이 가장 먼저 노출됩니다.", example = "1")
    private final Integer displayOrder;

    public FeaturedMatchResponse(
            Long featuredMatchId,
            Long matchId,
            String leagueName,
            String homeTeamName,
            String awayTeamName,
            LocalDateTime startAt,
            FeaturedMatchType type,
            Integer displayOrder) {

        this.featuredMatchId = featuredMatchId;
        this.matchId = matchId;
        this.leagueName = leagueName;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.startAt = startAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.type = type.name();
        this.displayOrder = displayOrder;
    }
}
