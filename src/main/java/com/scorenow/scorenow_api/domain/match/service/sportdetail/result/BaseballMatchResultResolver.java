package com.scorenow.scorenow_api.domain.match.service.sportdetail.result;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.springframework.stereotype.Component;

@Component
public class BaseballMatchResultResolver implements MatchResultResolver {
    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }

    @Override
    public MatchResult resolveResult(Match match, MatchDetailDocument matchDetailDocument) {
        return MatchResult.fromScore(
                match.getHomeScore(),
                match.getAwayScore()
        );
    }
}
