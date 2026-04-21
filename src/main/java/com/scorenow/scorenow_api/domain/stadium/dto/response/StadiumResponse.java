package com.scorenow.scorenow_api.domain.stadium.dto.response;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StadiumResponse {
    @Schema(description = "경기장 ID", example = "1")
    private Long id;
    @Schema(description = "외부 API 경기장 ID", example = "686")
    private String externalStadiumId;
    @Schema(description = "경기장명", example = "Neo Quimica Arena")
    private String name;
    @Schema(description = "종목ID", example = "1")
    private String sportId;
    @Schema(description = "도시명", example = "Sao Paulo")
    private String city;

    public static StadiumResponse from(Stadium stadium) {
        return StadiumResponse.builder()
                .id(stadium.getId())
                .externalStadiumId(stadium.getExternalStadiumId())
                .name(stadium.getName())
                .sportId(stadium.getSportId())
                .city(stadium.getCity())
                .build();
    }
}
