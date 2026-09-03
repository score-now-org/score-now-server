package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;

public interface SportDetailEventProcessor {
    SportDetailType type();

    void process(MatchDetailDocument currentMatchDetailDocument, MatchDetailDocument newMatchDetailDocument);
}
