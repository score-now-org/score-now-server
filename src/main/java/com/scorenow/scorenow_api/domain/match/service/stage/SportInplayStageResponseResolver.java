package com.scorenow.scorenow_api.domain.match.service.stage;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;

public interface SportInplayStageResponseResolver {
    SportDetailType type();
    MatchStageResponse resolve(Match match, MatchDetailDocument matchDetailDocument);
}
