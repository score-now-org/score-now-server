package com.scorenow.scorenow_api.domain.match.dto.sync;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public record MatchDetailSyncSnapshot(
        DataOrigin dataOrigin,
        SportCode sportCode,
        Long matchId,
        String externalSportId,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status,
        MatchDetailSyncStadium stadium,
        SportDetail sportDetail
) {
}
