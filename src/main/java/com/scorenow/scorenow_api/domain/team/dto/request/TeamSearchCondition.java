package com.scorenow.scorenow_api.domain.team.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamSearchCondition {

    private Long teamId;
    private String kName;
    private String eName;
}
