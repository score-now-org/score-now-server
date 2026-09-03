package com.scorenow.scorenow_api.domain.match.service.statusdisplay;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;

public interface InplayStatusDisplayResolver {
    SportDetailType type();
    MatchStatusDisplayResponse resolve(Match match, MatchDetailDocument matchDetailDocument);
}
