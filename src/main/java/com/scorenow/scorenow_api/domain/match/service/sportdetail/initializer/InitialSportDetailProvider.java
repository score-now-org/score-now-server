package com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface InitialSportDetailProvider {

    SportCode type();

    SportDetail create();
}
