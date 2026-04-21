package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssignTemporaryStadiumRequest {
    @NotBlank(message = "임시 경기장 이름은 필수입니다.")
    private String stadiumName;
    
    private String city;
}
