package com.scorenow.scorenow_api.domain.match.service.statusdisplay;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface InplayStatusDisplayResolver {
    SportCode type();
    MatchStatusDisplayResponse resolve(Match match, MatchDetailDocument matchDetailDocument);
}
