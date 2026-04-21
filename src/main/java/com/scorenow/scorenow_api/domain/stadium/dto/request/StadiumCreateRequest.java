package com.scorenow.scorenow_api.domain.stadium.dto.request;

import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StadiumCreateRequest {
    @NotBlank(message = "경기장 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "경기장ID 는 필수입니다.")
    private String externalStadiumId;

    @NotBlank(message = "종목ID 는 필수입니다.")
    private String sportId;
    private String city;

    public Stadium toEntity() {
        return Stadium.of(externalStadiumId, name, sportId, city);
    }
}
