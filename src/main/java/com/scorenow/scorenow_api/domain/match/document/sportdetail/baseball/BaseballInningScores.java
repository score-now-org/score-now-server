package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Getter
public class BaseballInningScores {

    private final Map<BaseballHalfInning, Integer> scoresByHalfInning;

    private BaseballInningScores(Map<BaseballHalfInning, Integer> scoresByHalfInning) {
        this.scoresByHalfInning = Collections.unmodifiableMap(scoresByHalfInning);
    }

    public static BaseballInningScores initialize() {
        Map<BaseballHalfInning, Integer> inningScores = new EnumMap<>(BaseballHalfInning.class);

        for (BaseballHalfInning inning : BaseballHalfInning.values()) {
            inningScores.put(inning, null);
        }

        return new BaseballInningScores(inningScores);
    }

    public static BaseballInningScores of(Map<BaseballHalfInning, Integer> scores) {
        if (scores == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이닝별 점수는 필수입니다.");
        }

        Map<BaseballHalfInning, Integer> copy = new EnumMap<>(BaseballHalfInning.class);
        for (var entry : scores.entrySet()) {
            if (entry.getKey() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이닝 정보는 필수입니다.");
            }

            Integer score = entry.getValue();
            if (score != null && score < 0) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "점수는 음수일 수 없습니다.");
            }

            copy.put(entry.getKey(), entry.getValue());
        }

        // 점수가 비어있는 이닝이 있다면 해당 이닝의 경우 null 로 채워준다.
        for (BaseballHalfInning halfInning : BaseballHalfInning.values()) {
            copy.putIfAbsent(halfInning, null);
        }

        return new BaseballInningScores(copy);
    }

    /**
     * 홈팀 총점을 계산한다. (Bottom)
     */
    public int totalHomeScore() {
        int total = 0;
        for (BaseballHalfInning halfInning : scoresByHalfInning.keySet()) {
            if (halfInning.getHalf() == BaseballHalfInning.Half.BOTTOM) {
                Integer score = scoresByHalfInning.get(halfInning);
                total += score != null ? score : 0;
            }
        }

        return total;
    }

    /**
     * 어웨이팀 총점을 계산한다. (Top)
     */
    public int totalAwayScore() {
        int total = 0;
        for (BaseballHalfInning halfInning : scoresByHalfInning.keySet()) {
            if (halfInning.getHalf() == BaseballHalfInning.Half.TOP) {
                Integer score = scoresByHalfInning.get(halfInning);
                total += score != null ? score : 0;
            }
        }

        return total;
    }

    /**
     * 이닝 별 점수 정보를 활용하여 현재 이닝 정보를 역산한다.
     */
    public BaseballHalfInning resolveCurrentHalfInning() {
        int minInning = BaseballHalfInning.getMinInning();
        int maxInning = BaseballHalfInning.getMaxInning();

        for (int inning = minInning; inning <= maxInning; inning++) {
            BaseballHalfInning currentTopInning = BaseballHalfInning.getTopInning(inning);
            BaseballHalfInning currentBottomInning = BaseballHalfInning.getBottomInning(inning);

            // 1. 어웨이(TOP) 점수가 있고, 홈(BOTTOM) 점수는 없다면, 현재는 N회 초이다.
            if (scoresByHalfInning.get(currentTopInning) != null && scoresByHalfInning.get(currentBottomInning) == null) {
                return currentTopInning;
            }

            // 2. 어웨이(TOP) 점수가 있고, 홈(BOTTOM) 점수도 있고, 다음 이닝 어웨이(TOP) 점수가 없다면, 현재는 N회 말이다.
            if (scoresByHalfInning.get(currentTopInning) != null && scoresByHalfInning.get(currentBottomInning) != null) {

                BaseballHalfInning nextTopInning = BaseballHalfInning.getTopInning(inning + 1);

                // 다음 이닝 초(TOP) 정보가 없는 경우 (최대 이닝인 20회를 넘어가는 경우)
                if (nextTopInning == null) {
                    return currentBottomInning;
                }

                // 다음 이닝 초(TOP) 점수가 없는 경우
                if (scoresByHalfInning.get(nextTopInning) == null) {
                    return currentBottomInning;
                }
            }
        }

        return null;
    }

    public Integer getHalfInningScore(BaseballHalfInning halfInning) {
        return scoresByHalfInning.get(halfInning);
    }

}
