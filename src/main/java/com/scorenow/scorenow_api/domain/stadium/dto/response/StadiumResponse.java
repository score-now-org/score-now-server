package com.scorenow.scorenow_api.domain.stadium.dto.response;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StadiumResponse {
    private Long id;
    private String externalStadiumId;
    private String name;
    private String sportId;
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
