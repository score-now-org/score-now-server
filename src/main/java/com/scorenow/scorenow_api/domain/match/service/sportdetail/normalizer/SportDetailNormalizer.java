package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;

public interface SportDetailNormalizer {
    SportDetailType type();

    SportDetail normalize(BetsViewResponse.ViewResult viewResult);
}
