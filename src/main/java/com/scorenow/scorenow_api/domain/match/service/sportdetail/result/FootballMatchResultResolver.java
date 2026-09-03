package com.scorenow.scorenow_api.domain.match.service.sportdetail.result;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import org.springframework.stereotype.Component;

@Component
public class FootballMatchResultResolver implements MatchResultResolver {

    @Override
    public SportDetailType type() {
        return SportDetailType.FOOTBALL;
    }

    @Override
    public MatchResult resolveResult(Match match, MatchDetailDocument matchDetailDocument) {
        FootballDetail footballDetail = resolveFootballDetail(matchDetailDocument);
        FootballShootOutScore shootOutScore = footballDetail.getShootOutScore();

        if (hasShootOutScore(shootOutScore)) {
            return MatchResult.fromScore(
                    shootOutScore.getHomeScore(),
                    shootOutScore.getAwayScore());
        }

        return MatchResult.fromScore(
                match.getHomeScore(),
                match.getAwayScore());
    }

    private FootballDetail resolveFootballDetail(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null) {
            return FootballDetail.empty();
        }

        SportDetail sportDetail = matchDetailDocument.getSportDetail();
        if (sportDetail instanceof FootballDetail footballDetail) {
            return footballDetail;
        }

        return FootballDetail.empty();
    }

    private boolean hasShootOutScore(FootballShootOutScore shootOutScore) {
        return shootOutScore != null
                && shootOutScore.getHomeScore() != null
                && shootOutScore.getAwayScore() != null
                && !shootOutScore.getHomeScore().equals(shootOutScore.getAwayScore());
    }
}
