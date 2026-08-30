package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;

public interface MatchStatisticsResolver {
    SportDetailType type();

    MatchStatisticsResponse resolve(MatchDetailDocument matchDetailDocument);
}
