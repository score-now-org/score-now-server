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
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
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

        if (clock == null || clock.getPhase() == null) {
            return MatchStageResponse.builder()
                    .displayText(match.getStatusCode().getDescription())
                    .build();
        }

        return MatchStageResponse.builder()
                .displayText(footballClockDisplayTextResolver.resolve(clock))
                .detail(FootballInplayStageDetailResponse.from(clock))
                .build();
    }

    private FootballClock resolveFootballClock(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null || matchDetailDocument.getSportDetail() == null) {
            return null;
        }

        SportDetail sportDetail = matchDetailDocument.getSportDetail();

        if (!(sportDetail instanceof FootballDetail footballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 상세 정보가 아닙니다.");
        }

        if (footballDetail.getClock() == null) {
            return FootballClock.empty();
        }

        return footballDetail.getClock();
    }
}
