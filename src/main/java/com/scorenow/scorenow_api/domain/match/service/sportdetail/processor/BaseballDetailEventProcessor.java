package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BaseballDetailEventProcessor implements SportDetailEventProcessor {

    private final MatchRealtimeEventPublisher eventPublisher;

    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
    }

    @Override
    public void process(MatchDetailDocument currentMatchDetailDocument, MatchDetailDocument newMatchDetailDocument) {
        BaseballDetail currentDetail = resolveBaseballDetail(currentMatchDetailDocument.getSportDetail());
        BaseballDetail newDetail = resolveBaseballDetail(newMatchDetailDocument.getSportDetail());

        if (shouldPublishEvent(currentDetail, newDetail)) {
            eventPublisher.publishBaseballDetailChanged(newMatchDetailDocument.getId(), newDetail);
        }
    }

    private boolean shouldPublishEvent(BaseballDetail currentDetail, BaseballDetail newDetail) {

        boolean commonChanged = isCommonChanged(currentDetail, newDetail);
        boolean detailChanged = newDetail.isDetailedBroadcastEnabled() && isDetailChanged(currentDetail, newDetail);

        return commonChanged || detailChanged;
    }

    private boolean isCommonChanged(BaseballDetail currentDetail, BaseballDetail newDetail) {
        // 이닝 정보가 바뀐 경우
        if (currentDetail.getCurrentHalfInning() != newDetail.getCurrentHalfInning()) {
            return true;
        }

        // 이닝 별 점수가 바뀐 경우
        BaseballInningScores currentDetailInningScores = currentDetail.getInningScores();
        BaseballInningScores newDetailInningScores = newDetail.getInningScores();

        // 둘 다 이닝 별 점수가 null 인 경우
        if (currentDetailInningScores == newDetailInningScores) {
            return false;
        }

        // 둘 중 하나만 null 인 경우
        if (currentDetailInningScores == null || newDetailInningScores == null) {
            return true;
        }

        for (BaseballHalfInning halfInning : BaseballHalfInning.values()) {
            Integer currentScore = currentDetailInningScores.getHalfInningScore(halfInning);
            Integer newScore = newDetailInningScores.getHalfInningScore(halfInning);

            if (!Objects.equals(currentScore, newScore)) {
                return true;
            }
        }

        return false;
    }

    private boolean isDetailChanged(BaseballDetail currentDetail, BaseballDetail newDetail) {
        // 투수 변경
        boolean pitcherChanged = !Objects.equals(currentDetail.getPitcher(), newDetail.getPitcher());

        // 타자 변경
        boolean batterChanged = !Objects.equals(currentDetail.getBatter(), newDetail.getBatter());

        // 진루 현황 변경
        BaseballBaseStates currentBaseStates = currentDetail.getBaseStates();
        BaseballBaseStates newBaseStates = newDetail.getBaseStates();
        boolean baseStatesChanged = isBaseStatesChanged(currentBaseStates, newBaseStates);

        // 볼,아웃 카운트 변경
        BaseballCount currentCount = currentDetail.getCount();
        BaseballCount newCount = newDetail.getCount();
        boolean countChanged = isCountChanged(currentCount, newCount);

        return pitcherChanged || batterChanged || baseStatesChanged || countChanged;
    }

    private boolean isBaseStatesChanged(BaseballBaseStates currentBaseStates, BaseballBaseStates newBaseStates) {
        // 둘 다 진루 상태가 null 인 경우
        if (currentBaseStates == newBaseStates) {
            return false;
        }

        // 둘 중 하나만 null 인 경우
        if (currentBaseStates == null || newBaseStates == null) {
            return true;
        }

        for (BaseballBase base : BaseballBase.values()) {
            BaseballBaseStates.BaseballBaseState currentBaseState = currentBaseStates.getBaseState(base);
            BaseballBaseStates.BaseballBaseState newBaseState = newBaseStates.getBaseState(base);

            if (currentBaseState.isRunnerOnBase() != newBaseState.isRunnerOnBase()) {
                return true;
            }

            if (!Objects.equals(currentBaseState.getRunnerName(), newBaseState.getRunnerName())) {
                return true;
            }
        }

        return false;
    }

    private boolean isCountChanged(BaseballCount currentCount, BaseballCount newCount) {
        // 둘 다 카운트 상태가 null 인 경우
        if (currentCount == newCount) {
            return false;
        }

        // 둘 중 하나만 null 인 경우
        if (currentCount == null || newCount == null) {
            return true;
        }

        boolean ballsChanged = currentCount.getBalls() != newCount.getBalls();
        boolean strikesChanged = currentCount.getStrikes() != newCount.getStrikes();
        boolean outsChanged = currentCount.getOuts() != newCount.getOuts();

        return ballsChanged || strikesChanged || outsChanged;
    }

    private static BaseballDetail resolveBaseballDetail(SportDetail sportDetail) {
        if (sportDetail == null) {
            return BaseballDetail.empty();
        }

        if (!(sportDetail instanceof BaseballDetail baseballDetail)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "야구 상세 정보가 아닙니다.");
        }

        return baseballDetail;
    }
}
