package com.scorenow.scorenow_api.domain.stadium.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StadiumCreateRequest {
    @NotBlank(message = "경기장 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "경기장ID 는 필수입니다.")
    private Long externalStadiumId;

    @NotBlank(message = "종목ID 는 필수입니다.")
    private Long sportId;
    private String city;

    //TODO: 추후 경기장 수동 등록 관리자 페이지 개발 시 개발 예정
//    public Stadium toEntity() {
//        return Stadium.of(externalStadiumId, name, sportId, city);
//    }
}
