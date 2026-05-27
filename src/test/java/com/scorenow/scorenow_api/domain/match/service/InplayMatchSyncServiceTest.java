package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.redis.InplayMatchRedisRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.scorenow.scorenow_api.external.common.ApiProvider.BETS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InplayMatchSyncServiceTest {

    @InjectMocks
    private InplayMatchSyncService service;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private InplayMatchStatusUpdater inplayMatchStatusUpdater;

    @Mock
    private InplayMatchRedisRepository inplayMatchRedisRepository;

    @Test
    void API_응답을_대조하여_진행_중인_경기와_유예기간_초과_경기를_정상적으로_분류하고_반영한다() {
        ZonedDateTime now = ZonedDateTime.of(2026, 1, 1, 13, 30, 0, 0, ZoneId.of("Asia/Seoul"));

        // 동기화 대상 조회 결과 세팅 현재 시작 예정 경기 세팅 (축구 2건 + 야구 1건 + 농구 1건) / 농구의 경우 유예기간 초과 경기
        MatchCandidate soccerCandidate1 = new MatchCandidate(1L, 1L, BETS, "100", "SOCCER", now.toLocalDateTime());
        MatchCandidate soccerCandidate2 = new MatchCandidate(2L, 1L, BETS, "101", "SOCCER", now.toLocalDateTime());
        MatchCandidate baseballCandidate1 = new MatchCandidate(3L, 2L, BETS, "200", "BASEBALL", now.toLocalDateTime());
        MatchCandidate basketballCandidate1 = new MatchCandidate(4L, 3L, BETS, "300", "BASKETBALL", now.minusMinutes(20).toLocalDateTime());

        given(inplayMatchRedisRepository.findCandidatesToSync(now)).willReturn(List.of(soccerCandidate1, soccerCandidate2, baseballCandidate1, basketballCandidate1));

        // INPLAY API 응답값 세팅
        BetsEventResponse inplaySoccer = getBetsEventResponse(List.of("100", "101"));
        BetsEventResponse inplayBaseball = getBetsEventResponse(List.of("200"));
        BetsEventResponse inplayBasketball = getBetsEventResponse(Collections.emptyList());  // 유예기간 초과의 경우

        given(betsApiClient.getInplayEvents("SOCCER", null)).willReturn(inplaySoccer);
        given(betsApiClient.getInplayEvents("BASEBALL", null)).willReturn(inplayBaseball);
        given(betsApiClient.getInplayEvents("BASKETBALL", null)).willReturn(inplayBasketball);

        service.syncInplayMatches(now.toLocalDateTime());

        // API 총 3회 호출 (축구 1회, 야구 1회, 농구 1회)
        then(betsApiClient).should(times(1)).getInplayEvents("SOCCER", null);
        then(betsApiClient).should(times(1)).getInplayEvents("BASEBALL", null);
        then(betsApiClient).should(times(1)).getInplayEvents("BASKETBALL", null);

        // InplayMatchStatusUpdater.updateMatchStatuses 1회 호출 검증
        then(inplayMatchStatusUpdater).should(times(1)).updateMatchStatuses(any(), any());

        // InplayMatchRedisRepository.removeCandiates 2회 호출 검증
        then(inplayMatchRedisRepository).should(times(2)).removeCandidates(any());
    }

    private BetsEventResponse getBetsEventResponse(List<String> apiMatchIds) {
        List<BetsEventResponse.Event> events = new ArrayList<>();
        for (String apiMatchId : apiMatchIds) {
            BetsEventResponse.Event event = new BetsEventResponse.Event();
            event.setId(apiMatchId);

            events.add(event);
        }

        BetsEventResponse betsEventResponse = new BetsEventResponse();
        betsEventResponse.setResults(events);
        return betsEventResponse;
    }
}