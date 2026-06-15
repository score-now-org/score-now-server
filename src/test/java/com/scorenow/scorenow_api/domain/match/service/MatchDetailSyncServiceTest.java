package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.ViewResult;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchDetailSyncServiceTest {

    @InjectMocks
    private MatchDetailSyncService matchDetailSyncService;

    @Mock
    private BetsApiClient betsApiClient;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchLineupRepository matchLineupRepository;
    @Mock
    private MatchDetailService matchDetailService;
    @Mock
    private InplayRedisService inplayRedisService;
    @Mock
    private MatchLineupGoalsService matchLineupGoalsService;

    // =========================================================================
    // 1. 조기 종료 (Early Return) 관련
    // =========================================================================

    @Test
    @DisplayName("진행 중(IN_PLAY)인 경기가 없으면 API를 호출하지 않고 조기 종료한다.")
    void syncInplayMatchDetails_WhenNoInplayMatches_ThenReturnEarly() {
        // given
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY))
                .willReturn(Collections.emptyList());

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(betsApiClient, never()).getEventView(anyString());
        verify(matchDetailService, never()).updateInplayMatchDetail(any(), any());
    }

    // =========================================================================
    // 2. 외부 API 호출 관련
    // =========================================================================

    @Test
    @DisplayName("조회된 경기가 10개를 초과할 경우 API를 10개 단위로 분할하여 호출한다.")
    void syncInplayMatchDetails_WhenMatchesExceedBatchSize_ThenCallApiInBatches() {
        // given
        List<Match> matches = createMockMatches(12);
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(matches);

        given(betsApiClient.getEventView(anyString())).willReturn(createMockResponse());

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        // 12개이므로 10개짜리 1번, 2개짜리 1번 총 2번 호출되어야 함
        verify(betsApiClient, times(2)).getEventView(anyString());
    }

    @Test
    @DisplayName("여러 번의 외부 API 호출 중 일부가 실패하더라도 다음 외부 API 호출은 정상 진행된다.")
    void syncInplayMatchDetails_WhenOneBatchFails_ThenContinueNextBatch() {
        // given
        List<Match> matches = createMockMatches(12); // 배치 2개 생성됨
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(matches);

        // 첫 번째 외부 API 호출 시 예외 발생, 두 번째 외부 API 호출 시 정상 응답하도록 설정
        given(betsApiClient.getEventView(anyString()))
                .willThrow(new RuntimeException("외부 API 호출중 Timeout 발생"))
                .willReturn(createMockResponse());

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(betsApiClient, times(2)).getEventView(anyString()); // 예외가 나도 두 번 모두 호출됨
    }

    // =========================================================================
    // 3. 비즈니스 처리 관련
    // =========================================================================

    @Test
    @DisplayName("API 응답에 해당 경기 상세 정보가 누락된 경우 처리를 스킵한다.")
    void syncInplayMatchDetails_WhenMatchDetailIsMissingInApi_ThenSkip() {
        // given
        Match match = createMockMatch(1L, "api-100");
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(List.of(match));

        BetsViewResponse emptyResponse = new BetsViewResponse();
        emptyResponse.setResults(Collections.emptyList()); // 응답에 결과가 없음
        given(betsApiClient.getEventView("api-100")).willReturn(emptyResponse);

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(matchDetailService, never()).updateInplayMatchDetail(any(), any());
    }

    @Test
    @DisplayName("API 응답에 라인업 정보가 없는 경우 Detail만 업데이트하고 라인업-골 동기화 로직은 스킵한다.")
    void syncInplayMatchDetails_WhenNoLineupProvided_ThenSkipLineupGoalLogic() {
        // given
        Match match = createMockMatch(1L, "api-100");
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(List.of(match));

        ViewResult viewResult = createMockViewResult("api-100", false); // 라인업 제공 안함
        BetsViewResponse response = createMockResponse(viewResult);
        given(betsApiClient.getEventView("api-100")).willReturn(response);

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(matchDetailService, times(1)).updateInplayMatchDetail(DataOrigin.BETS, viewResult);
        verify(inplayRedisService, never()).markSeenAndEnqueue(anyString(), anyString());
        verify(matchLineupGoalsService, never()).updateLineupGoals((MatchLineupDocument) any(), any());
    }

    @Test
    @DisplayName("라인업은 제공되었으나 MatchLineupDocument가 없으면 Redis 의 NEW_LINEUP_QUEUE에 삽입하고 루프를 종료한다.")
    void syncInplayMatchDetails_WhenLineupProvidedButNoLocalDoc_ThenEnqueueToRedis() {
        // given
        Match match = createMockMatch(1L, "api-100");
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(List.of(match));

        ViewResult viewResult = createMockViewResult("api-100", true);
        BetsViewResponse response = createMockResponse(viewResult);
        given(betsApiClient.getEventView("api-100")).willReturn(response);

        // 로컬 DB에 라인업 문서가 없는 상태
        given(matchLineupRepository.findByIdIn(List.of(1L))).willReturn(Collections.emptyList());
        given(inplayRedisService.markSeenAndEnqueue("1", "10")).willReturn(true);

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(matchDetailService, times(1)).updateInplayMatchDetail(DataOrigin.BETS, viewResult);
        verify(inplayRedisService, times(1)).markSeenAndEnqueue("1", "10");
        verify(matchLineupGoalsService, never()).updateLineupGoals((MatchLineupDocument) any(), any()); // 골 업데이트는 스킵됨
    }

    @Test
    @DisplayName("라인업이 제공되고 MatchLineupDocument도 이미 존재하면 라인업-골 동기화 로직을 수행한다.")
    void syncInplayMatchDetails_WhenLineupProvidedAndLocalDocExists_ThenUpdateLineupGoals() {
        // given
        Match match = createMockMatch(1L, "api-100");
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY)).willReturn(List.of(match));

        ViewResult viewResult = createMockViewResult("api-100", true);
        BetsViewResponse response = createMockResponse(viewResult);
        given(betsApiClient.getEventView("api-100")).willReturn(response);

        MatchLineupDocument document = mock(MatchLineupDocument.class);
        given(document.getId()).willReturn(1L);
        given(matchLineupRepository.findByIdIn(List.of(1L))).willReturn(List.of(document));

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        verify(matchDetailService, times(1)).updateInplayMatchDetail(DataOrigin.BETS, viewResult);
        verify(inplayRedisService, never()).markSeenAndEnqueue(anyString(), anyString()); // 큐 삽입 안함
        verify(matchLineupGoalsService, times(1)).updateLineupGoals(document, viewResult);
    }

    // =========================================================================
    // 4. 예외 격리 관련
    // =========================================================================

    @Test
    @DisplayName("특정 경기 세부 정보 업데이트 중 예외가 발생해도, 다음 경기의 업데이트는 정상적으로 진행된다.")
    void syncInplayMatchDetails_WhenExceptionDuringUpdate_ThenContinueWithNextMatch() {
        // given
        Match match1 = createMockMatch(1L, "api-100");
        Match match2 = createMockMatch(2L, "api-200");
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY))
                .willReturn(List.of(match1, match2));

        ViewResult result1 = createMockViewResult("api-100", false);
        ViewResult result2 = createMockViewResult("api-200", false);
        BetsViewResponse response = createMockResponse(result1, result2);
        given(betsApiClient.getEventView("api-100,api-200")).willReturn(response);

        // 첫 번째 경기 업데이트 시 예외 발생 유도
        willThrow(new RuntimeException("DB Deadlock"))
                .given(matchDetailService).updateInplayMatchDetail(DataOrigin.BETS, result1);

        // when
        matchDetailSyncService.syncInplayMatchDetails();

        // then
        // match1 업데이트에서 예외가 났지만, match2 업데이트까지 도달해서 총 2번의 호출 시도가 있었는지 검증
        verify(matchDetailService, times(1)).updateInplayMatchDetail(DataOrigin.BETS, result1);
        verify(matchDetailService, times(1)).updateInplayMatchDetail(DataOrigin.BETS, result2);
    }


    private Match createMockMatch(Long id, String apiMatchId) {
        Match match = mock(Match.class);
        lenient().when(match.getId()).thenReturn(id);
        lenient().when(match.getSportId()).thenReturn(10L); // Default Sport ID
        lenient().when(match.getApiMatchId()).thenReturn(apiMatchId);
        return match;
    }

    private List<Match> createMockMatches(int count) {
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            matches.add(createMockMatch((long) i, "api-" + i));
        }
        return matches;
    }

    private ViewResult createMockViewResult(String id, boolean hasLineup) {
        ViewResult result = mock(ViewResult.class);
        lenient().when(result.getId()).thenReturn(id);
        lenient().when(result.hasLineup()).thenReturn(hasLineup);
        return result;
    }

    private BetsViewResponse createMockResponse() {
        BetsViewResponse betsViewResponse = new BetsViewResponse();
        betsViewResponse.setResults(new ArrayList<>());
        return betsViewResponse;
    }

    private BetsViewResponse createMockResponse(ViewResult... results) {
        BetsViewResponse response = new BetsViewResponse();
        response.setResults(List.of(results));
        return response;
    }
}