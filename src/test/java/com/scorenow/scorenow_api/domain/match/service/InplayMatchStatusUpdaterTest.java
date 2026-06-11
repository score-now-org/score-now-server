package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.IN_PLAY;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.TO_BE_FIXED;
import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InplayMatchStatusUpdaterTest {
    @InjectMocks
    private InplayMatchStatusUpdater service;

    @Mock
    private MatchRepository matchRepository;

    @Test
    void 진행_중인_경기와_유예기간_초과한_경기에_맞춰_일괄로_경기_상태_업데이트를_처리한다() {
        MatchCandidate inplayCandidate1 = new MatchCandidate(1L, 1L, BETS, "100", "SOCCER", LocalDateTime.now());
        MatchCandidate inplayCandidate2 = new MatchCandidate(2L, 1L, BETS, "101", "SOCCER", LocalDateTime.now());
        MatchCandidate toBeFixedCandidate = new MatchCandidate(3L, 1L, BETS, "103", "SOCCER", LocalDateTime.now().minusMinutes(20));

        List<MatchCandidate> inplayCandidates = List.of(inplayCandidate1, inplayCandidate2);
        List<MatchCandidate> toBeFixedCandidates = List.of(toBeFixedCandidate);

        service.updateMatchStatuses(inplayCandidates, toBeFixedCandidates);

        List<Long> inplayCandidateIds = inplayCandidates.stream().map(MatchCandidate::getMatchId).toList();
        List<Long> toBeFixedCandidateIds = toBeFixedCandidates.stream().map(MatchCandidate::getMatchId).toList();

        // MatchRepository.updateStatusBulk(any(), IN_PLAY) 1회 호출 검증
        then(matchRepository).should(times(1)).updateStatusBulk(eq(inplayCandidateIds), eq(IN_PLAY));
        // MatchRepository.updateStatusBulk(any(), TO_BE_FIXED) 1회 호출 검증
        then(matchRepository).should(times(1)).updateStatusBulk(eq(toBeFixedCandidateIds), eq(TO_BE_FIXED));
    }

    @Test
    void 진행_중인_경기만_있고_유예기간_초과한_경기는_없는_경우_진행_중인_경기에_대해서만_상태_업데이트를_처리한다() {
        MatchCandidate inplayCandidate = new MatchCandidate(1L, 1L, BETS, "100", "SOCCER", LocalDateTime.now());

        service.updateMatchStatuses(List.of(inplayCandidate), Collections.emptyList());

        // MatchRepository.updateStatusBulk(any(), IN_PLAY) 1회 호출 검증
        then(matchRepository).should(times(1)).updateStatusBulk(any(), eq(IN_PLAY));
        // MatchRepository.updateStatusBulk(any(), TO_BE_FIXED) 0회 호출 검증
        then(matchRepository).should(times(0)).updateStatusBulk(any(), eq(TO_BE_FIXED));
    }

}