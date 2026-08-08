package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayCandidateStatusUpdateService {

    private final MatchRepository matchRepository;
    private final MatchRealtimeEventPublisher eventPublisher;

    @Transactional
    public int updateInplayStatuses(List<MatchCandidate> candidates) {
        return transitionStatus(candidates, IN_PLAY);
    }

    @Transactional
    public int updateToBeFixedStatuses(List<MatchCandidate> candidates) {
        return transitionStatus(candidates, TO_BE_FIXED);
    }

    private int transitionStatus(List<MatchCandidate> candidates, MatchStatus matchStatus) {
        if (candidates.isEmpty()) {
            return 0;
        }

        List<Long> matchIds = candidates.stream()
                .map(MatchCandidate::getMatchId)
                .toList();

        int updatedCount = matchRepository.updateStatusBulk(matchIds, matchStatus);

        if (updatedCount > 0) {
            matchIds.forEach(matchId -> eventPublisher.publishMatchStatusChanged(matchId, matchStatus, matchStatus.getDescription()));
        }

        return updatedCount;
    }

}
