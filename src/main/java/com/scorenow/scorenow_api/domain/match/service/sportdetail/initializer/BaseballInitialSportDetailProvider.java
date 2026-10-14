package com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.springframework.stereotype.Component;

@Component
public class BaseballInitialSportDetailProvider implements InitialSportDetailProvider {

    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }

    @Override
    public SportDetail create() {
        return BaseballDetail.builder()
                .detailedBroadcastEnabled(false)
                .build();
    }
}
