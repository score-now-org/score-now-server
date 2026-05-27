package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.redis.InplayMatchRedisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;
import static com.scorenow.scorenow_api.external.common.ApiProvider.BETS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InplayMatchCandidateLoadServiceTest {

    @InjectMocks
    private InplayMatchCandidateLoadService service;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private InplayMatchRedisRepository inplayMatchRedisRepository;

    @Test
    void 시작_예정인_경기를_DB에서_조회하여_Redis에_캐싱한다() {
        ZonedDateTime now = ZonedDateTime.of(2026, 1, 1, 13, 0, 0, 0, ZoneId.of("Asia/Seoul"));

        // 1시간 이내 시작 예정 경기 2건 세팅
        MatchCandidate candidate1 = new MatchCandidate(1L, 1L, BETS, "100", "1", LocalDateTime.of(2026, 1, 1, 13, 30));
        MatchCandidate candidate2 = new MatchCandidate(2L, 1L, BETS, "101", "1", LocalDateTime.of(2026, 1, 1, 13, 45));
        List<MatchCandidate> candidates = List.of(candidate1, candidate2);

        // 1시간 이내 시작 예정인 경기 목록 조회 시 반환 값 세팅
        given(matchRepository.findMatchesStartingWithin(NOT_STARTED, now.toLocalDateTime(), now.plusHours(1L).toLocalDateTime()))
                .willReturn(candidates);

        service.loadInplayCandidatesToCache(now.toLocalDateTime());

        // MatchRepository.findMatchesStartingWithin 1회 호출되었음을 검증
        then(matchRepository).should(times(1)).findMatchesStartingWithin(any(), any(), any());

        // InplayMatchRedisRepository.addCandidate {candidates.size} 만큼 호출되었음을 검증
        then(inplayMatchRedisRepository).should(times(candidates.size())).addCandidate(any(), any());
    }
}