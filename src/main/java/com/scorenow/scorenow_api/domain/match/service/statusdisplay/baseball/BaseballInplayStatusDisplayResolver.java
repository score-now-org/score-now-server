package com.scorenow.scorenow_api.domain.match.service.statusdisplay.baseball;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.baseball.BaseballInplayStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.InplayStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.springframework.stereotype.Component;

@Component
public class BaseballInplayStatusDisplayResolver implements InplayStatusDisplayResolver {
    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }

    @Override
    public MatchStatusDisplayResponse resolve(Match match, MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null) {
            return MatchStatusDisplayResponse.builder()
                    .displayText(match.getStatusCode().getDescription())
                    .build();
        }

        BaseballDetail baseballDetail = resolveBaseballDetail(matchDetailDocument);
        BaseballHalfInning currentHalfInning = baseballDetail.getCurrentHalfInning();

        return MatchStatusDisplayResponse.builder()
                .displayText(currentHalfInning == null ? match.getStatusCode().getDescription() : currentHalfInning.getDescription())
                .detail(BaseballInplayStatusDisplayDetailResponse.from(currentHalfInning))
                .build();
    }

    private BaseballDetail resolveBaseballDetail(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null) {
            return BaseballDetail.empty();
        }

        SportDetail sportDetail = matchDetailDocument.getSportDetail();
        if (sportDetail instanceof BaseballDetail baseballDetail) {
            return baseballDetail;
        }

        return BaseballDetail.empty();
    }
}
