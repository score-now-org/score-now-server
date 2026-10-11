package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InplayMatchDetailSynchronizerRegistry {
    private final Map<SportCode, InplayMatchDetailSynchronizer> synchronizers;

    public InplayMatchDetailSynchronizerRegistry(List<InplayMatchDetailSynchronizer> synchronizerList) {
        this.synchronizers = synchronizerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                                InplayMatchDetailSynchronizer::type,
                                Function.identity()
                        )
                );
    }

    public void sync(SportCode sportCode, List<Match> inplayMatches) {
        InplayMatchDetailSynchronizer synchronizer = synchronizers.get(sportCode);

        if (synchronizer == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + sportCode.name());
        }

        if (inplayMatches == null || inplayMatches.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "동기화 대상 진행중 경기 목록이 비어있습니다.");
        }

        synchronizer.sync(sportCode, inplayMatches);
    }
}
