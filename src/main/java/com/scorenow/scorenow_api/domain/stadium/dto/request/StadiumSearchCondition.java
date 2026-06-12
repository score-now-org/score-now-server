package com.scorenow.scorenow_api.domain.stadium.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StadiumSearchCondition {

    private Long stadiumId;
    private String name;
}
