package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsBaseballViewResponse;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BaseballDetailNormalizer {

    public BaseballDetail normalize(BetsBaseballViewResponse.ViewResult viewResult) {
        BaseballInningScores inningScores = normalizeInningScores(viewResult);
        BaseballHalfInning halfInning = inningScores == null ? null : inningScores.resolveCurrentHalfInning();

        return BaseballDetail.builder()
                .currentHalfInning(halfInning)
                .inningScores(inningScores)
                .build();
    }

    /**
     * 야구 Bets View API 의 이닝 별 점수 정보를 활용하여 내부에서 사용하는 이닝별 점수 형태로 변환한다.
     */
    private BaseballInningScores normalizeInningScores(BetsBaseballViewResponse.ViewResult viewResult) {
        int minInning = BaseballHalfInning.getMinInning();
        int maxInning = BaseballHalfInning.getMaxInning();

        Map<BaseballHalfInning, Integer> scoresByHalfInning = new EnumMap<>(BaseballHalfInning.class);

        for (int inning = minInning; inning <= maxInning; inning++) {
            BetsBaseballViewResponse.Score currentInningScore = viewResult.getInningScore(inning);

            if (currentInningScore == null) {
                break;
            }

            Integer awayScore = parseScore(currentInningScore.getAway());
            Integer homeScore = parseScore(currentInningScore.getHome());

            scoresByHalfInning.put(BaseballHalfInning.getTopInning(inning), awayScore);
            scoresByHalfInning.put(BaseballHalfInning.getBottomInning(inning), homeScore);
        }

        if (scoresByHalfInning.isEmpty()) {
            return null;
        }

        return BaseballInningScores.of(scoresByHalfInning);
    }

    private boolean hasValidInningScore(String score) {
        return score != null && !score.isBlank();
    }

    private Integer parseScore(String score) {
        if (!hasValidInningScore(score)) {
            return null;
        }

        try {
            return Integer.parseInt(score.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
