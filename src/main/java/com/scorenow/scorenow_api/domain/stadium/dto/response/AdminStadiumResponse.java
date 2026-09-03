package com.scorenow.scorenow_api.domain.stadium.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 경기장 응답")
public class AdminStadiumResponse {
    @Schema(description = "경기장 ID", example = "1")
    private Long id;
    @Schema(description = "경기장명", example = "Neo Quimica Arena")
    private String name;
    @Schema(description = "종목 ID", example = "1")
    private Long sportId;
    @Schema(description = "종목 코드", example = "FOOTBALL")
    private SportCode sportCode;
    @Schema(description = "종목명", example = "축구")
    private String sportName;
    @Schema(description = "도시명", example = "Sao Paulo")
    private String city;
    @Schema(description = "데이터 원천", example = "BETS", allowableValues = {"BETS", "MANUAL"})
    private DataOrigin dataOrigin;

    public static AdminStadiumResponse from(Stadium stadium, Sport sport) {
        return AdminStadiumResponse.builder()
                .id(stadium.getId())
                .name(stadium.getName())
                .sportId(stadium.getSportId())
                .sportCode(sport.getSportCode())
                .sportName(sport.resolveSportName())
                .city(stadium.getCity())
                .dataOrigin(stadium.getDataOrigin())
                .build();
    }
}
