package com.scorenow.scorenow_api.domain.match.service.appresponse.baseball;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.appresponse.MatchAppSportResponseMapper;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class BaseballMatchAppSportResponseMapper implements MatchAppSportResponseMapper {

    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }

    @Override
    public MatchAppSportLiveSummaryResponse toLiveSummary(Match match, SportDetail sportDetail) {
        if (match == null || match.getStatusCode() != MatchStatus.IN_PLAY) {
            return null;
        }

        BaseballDetail baseballDetail = resolveBaseballDetail(sportDetail);
        if (baseballDetail == null || !baseballDetail.isDetailedBroadcastEnabled()) {
            return null;
        }

        return BaseballAppLiveSummaryResponse.from(baseballDetail);
    }

    @Override
    public MatchAppSportDetailResponse toDetail(SportDetail sportDetail) {
        return BaseballAppMatchDetailResponse.from(resolveBaseballDetail(sportDetail));
    }

    private BaseballDetail resolveBaseballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return null;
        }

        if (!(sportDetail instanceof BaseballDetail baseballDetail)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "야구 경기 상세 정보 형식이 올바르지 않습니다. sportType=" + sportDetail.type()
            );
        }

        return baseballDetail;
    }
}
