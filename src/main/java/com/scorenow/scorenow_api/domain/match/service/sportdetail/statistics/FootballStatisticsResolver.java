package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.football.FootballStatisticsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class FootballStatisticsResolver implements MatchStatisticsResolver {

    @Override
    public SportDetailType type() {
        return SportDetailType.FOOTBALL;
    }

    @Override
    public FootballStatisticsResponse resolve(MatchDetailDocument matchDetailDocument) {

        if (!(matchDetailDocument.getSportDetail() instanceof FootballDetail footballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 상세 정보가 아닙니다.");
        }

        return new FootballStatisticsResponse(
                footballDetail.getHomeStats(),
                footballDetail.getAwayStats()
        );
    }

}
