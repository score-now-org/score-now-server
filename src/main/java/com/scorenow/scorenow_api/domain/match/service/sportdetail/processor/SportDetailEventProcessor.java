package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface SportDetailEventProcessor {
    SportCode type();

    void process(MatchDetailDocument currentMatchDetailDocument, MatchDetailDocument newMatchDetailDocument);
}
