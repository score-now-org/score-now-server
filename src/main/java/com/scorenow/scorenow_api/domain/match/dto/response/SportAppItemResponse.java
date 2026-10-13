package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public record SportAppItemResponse(
        SportCode sportCode,
        String name
) {
    public static SportAppItemResponse from(Sport sport) {
        return new SportAppItemResponse(sport.getSportCode(), sport.getKName());
    }

}
