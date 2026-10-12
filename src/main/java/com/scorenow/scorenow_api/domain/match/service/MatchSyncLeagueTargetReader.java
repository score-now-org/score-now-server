package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.repository.MatchSyncLeagueTargetQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchSyncLeagueTargetReader {
    private final MatchSyncLeagueTargetQueryRepository queryRepository;

    public List<MatchSyncLeagueTarget> findEnabledLeagueTargets(DataOrigin dataOrigin) {
        return queryRepository.findAllEnabledByDataOrigin(dataOrigin);
    }
}
