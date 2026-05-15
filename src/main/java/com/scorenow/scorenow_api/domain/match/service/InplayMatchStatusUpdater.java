package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
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
public class InplayMatchStatusUpdater {

    private final MatchRepository matchRepository;

    @Transactional
    public void updateMatchStatuses(List<MatchCandidate> inplayMatches, List<MatchCandidate> toBeFixedMatches) {
        if (!inplayMatches.isEmpty()) {
            List<Long> matchIds = inplayMatches.stream()
                    .map(MatchCandidate::getMatchId)
                    .toList();

            matchRepository.updateStatusBulk(matchIds, IN_PLAY);
        }

        if (!toBeFixedMatches.isEmpty()) {
            List<Long> matchIds = toBeFixedMatches.stream()
                    .map(MatchCandidate::getMatchId)
                    .toList();

            matchRepository.updateStatusBulk(matchIds, TO_BE_FIXED);
        }
    }

}
