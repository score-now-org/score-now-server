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

}
