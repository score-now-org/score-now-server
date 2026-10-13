package com.scorenow.scorenow_api.domain.match.service.appresponse;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface MatchAppSportResponseMapper {

    SportCode type();

    MatchAppSportLiveSummaryResponse toLiveSummary(Match match, SportDetail sportDetail);

    MatchAppSportDetailResponse toDetail(SportDetail sportDetail);
}
