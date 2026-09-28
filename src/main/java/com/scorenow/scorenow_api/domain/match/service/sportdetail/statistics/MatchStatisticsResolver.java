package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface MatchStatisticsResolver {
    SportCode type();

    MatchStatisticsResponse resolve(MatchDetailDocument matchDetailDocument);
}
