package com.scorenow.scorenow_api.domain.match.service.stage.football;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.football.FootballInplayStageDetailResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.display.football.FootballClockDisplayTextResolver;
import com.scorenow.scorenow_api.domain.match.service.stage.SportInplayStageResponseResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FootballInplayStageResponseResolver implements SportInplayStageResponseResolver {

    private final FootballClockDisplayTextResolver footballClockDisplayTextResolver;

    @Override
    public SportDetailType type() {
        return SportDetailType.FOOTBALL;
    }

    @Override
    public MatchStageResponse resolve(Match match, MatchDetailDocument matchDetailDocument) {
        FootballClock clock = resolveFootballClock(matchDetailDocument);

        if (clock.getPhase() == null) {
            return MatchStageResponse.empty();
        }

        return MatchStageResponse.builder()
                .displayText(footballClockDisplayTextResolver.resolve(clock))
                .detail(FootballInplayStageDetailResponse.from(clock))
                .build();
    }

    private FootballClock resolveFootballClock(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null) {
            return FootballClock.empty();
        }

        SportDetail sportDetail = matchDetailDocument.getSportDetail();

        if (!(sportDetail instanceof FootballDetail footballDetail)) {
            return FootballClock.empty();
        }

        if (footballDetail.getClock() == null) {
            return FootballClock.empty();
        }

        return footballDetail.getClock();
    }
}
