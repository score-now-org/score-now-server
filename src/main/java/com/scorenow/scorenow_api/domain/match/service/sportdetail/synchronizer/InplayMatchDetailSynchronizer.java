package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

import java.util.List;

public interface InplayMatchDetailSynchronizer {
    SportCode type();

    void sync(SportCode sportCode, List<Match> inplayMatches);
}
