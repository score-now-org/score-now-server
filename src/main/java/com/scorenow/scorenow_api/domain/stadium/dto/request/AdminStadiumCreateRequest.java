package com.scorenow.scorenow_api.domain.stadium.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class  AdminStadiumCreateRequest {
    @NotNull(message = "종목 ID 는 필수입니다.")
    private Long sportId;

    @NotBlank(message = "경기장 이름은 필수입니다.")
    private String name;

    private String city;
}
