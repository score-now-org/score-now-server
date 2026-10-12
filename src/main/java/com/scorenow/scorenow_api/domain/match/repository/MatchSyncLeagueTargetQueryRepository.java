package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;

import java.util.List;

public interface MatchSyncLeagueTargetQueryRepository {
    List<MatchSyncLeagueTarget> findAllEnabledByDataOrigin(DataOrigin dataOrigin);
}
