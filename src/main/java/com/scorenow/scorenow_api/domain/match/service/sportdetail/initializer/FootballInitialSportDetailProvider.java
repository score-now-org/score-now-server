package com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.springframework.stereotype.Component;

@Component
public class FootballInitialSportDetailProvider implements InitialSportDetailProvider {

    @Override
    public SportCode type() {
        return SportCode.FOOTBALL;
    }

    @Override
    public SportDetail create() {
        return FootballDetail.empty();
    }
}
