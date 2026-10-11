package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupGoalsService;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.FootballDetailNormalizer;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsFootballViewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FootballInplayMatchDetailSynchronizerTest {

    @InjectMocks
    private FootballInplayMatchDetailSynchronizer synchronizer;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private MatchLineupRepository matchLineupRepository;

    @Mock
    private MatchDetailService matchDetailService;

    @Mock
    private InplayRedisService inplayRedisService;

    @Mock
    private MatchLineupGoalsService matchLineupGoalsService;

    @Mock
    private FootballDetailNormalizer normalizer;

    @Test
    @DisplayName("경기가 10개를 초과하면 View API를 10개 단위로 호출한다.")
    void sync_WhenMatchesExceedBatchSize_ThenCallApiInBatches() {
        List<Match> matches = createMatches(12);
        given(betsApiClient.getFootballEventView(anyString())).willReturn(createResponse());

        synchronizer.sync(SportCode.FOOTBALL, matches);

        verify(betsApiClient, times(2)).getFootballEventView(anyString());
        verify(betsApiClient).getFootballEventView(
                "api-1,api-2,api-3,api-4,api-5,api-6,api-7,api-8,api-9,api-10"
        );
        verify(betsApiClient).getFootballEventView("api-11,api-12");
    }

    @Test
    @DisplayName("일부 API 배치가 실패해도 다음 배치를 계속 호출한다.")
    void sync_WhenOneBatchFails_ThenContinueNextBatch() {
        List<Match> matches = createMatches(12);
        String firstBatch = "api-1,api-2,api-3,api-4,api-5,api-6,api-7,api-8,api-9,api-10";
        String secondBatch = "api-11,api-12";
        given(betsApiClient.getFootballEventView(firstBatch))
                .willThrow(new RuntimeException("외부 API 호출 중 Timeout 발생"));
        given(betsApiClient.getFootballEventView(secondBatch)).willReturn(createResponse());

        synchronizer.sync(SportCode.FOOTBALL, matches);

        verify(betsApiClient).getFootballEventView(firstBatch);
        verify(betsApiClient).getFootballEventView(secondBatch);
    }

    @Test
    @DisplayName("View API 응답에 경기 상세 정보가 없으면 해당 경기를 건너뛴다.")
    void sync_WhenMatchDetailIsMissing_ThenSkipMatch() {
        Match match = createMatch(1L, 10L, "api-1");
        given(betsApiClient.getFootballEventView("api-1")).willReturn(createResponse());

        synchronizer.sync(SportCode.FOOTBALL, List.of(match));

        verifyNoInteractions(normalizer, matchDetailService, matchLineupRepository);
    }

    @Test
    @DisplayName("라인업이 없으면 경기 상세만 업데이트한다.")
    void sync_WhenNoLineupProvided_ThenUpdateOnlyMatchDetail() {
        Match match = createMatch(1L, 10L, "api-1");
        BetsFootballViewResponse.ViewResult viewResult = createViewResult("api-1", false);
        given(betsApiClient.getFootballEventView("api-1")).willReturn(createResponse(viewResult));
        given(normalizer.normalize(viewResult)).willReturn(FootballDetail.empty());

        synchronizer.sync(SportCode.FOOTBALL, List.of(match));

        ArgumentCaptor<MatchDetailSyncSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(MatchDetailSyncSnapshot.class);
        verify(matchDetailService).updateInplayMatchDetail(snapshotCaptor.capture());
        verifyNoInteractions(matchLineupRepository, inplayRedisService, matchLineupGoalsService);

        MatchDetailSyncSnapshot snapshot = snapshotCaptor.getValue();
        assertThat(snapshot.matchId()).isEqualTo(1L);
        assertThat(snapshot.sportCode()).isEqualTo(SportCode.FOOTBALL);
        assertThat(snapshot.homeScore()).isEqualTo(1);
        assertThat(snapshot.awayScore()).isZero();
        assertThat(snapshot.status()).isEqualTo(MatchStatus.IN_PLAY);
    }

    @Test
    @DisplayName("라인업 응답은 있지만 저장된 문서가 없으면 Redis 동기화 큐에 추가한다.")
    void sync_WhenLineupDocumentIsMissing_ThenEnqueueLineupSync() {
        Match match = createMatch(1L, 10L, "api-1");
        BetsFootballViewResponse.ViewResult viewResult = createViewResult("api-1", true);
        given(betsApiClient.getFootballEventView("api-1")).willReturn(createResponse(viewResult));
        given(matchLineupRepository.findByIdIn(List.of(1L))).willReturn(List.of());
        given(normalizer.normalize(viewResult)).willReturn(FootballDetail.empty());
        given(inplayRedisService.markSeenAndEnqueue("1", "10")).willReturn(true);

        synchronizer.sync(SportCode.FOOTBALL, List.of(match));

        verify(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
        verify(inplayRedisService).markSeenAndEnqueue("1", "10");
        verifyNoInteractions(matchLineupGoalsService);
    }

    @Test
    @DisplayName("라인업 문서가 있으면 라인업의 득점 정보를 동기화한다.")
    void sync_WhenLineupDocumentExists_ThenUpdateLineupGoals() {
        Match match = createMatch(1L, 10L, "api-1");
        BetsFootballViewResponse.ViewResult viewResult = createViewResult("api-1", true);
        MatchLineupDocument lineupDocument = MatchLineupDocument.create(1L, 10L, "api-1");
        given(betsApiClient.getFootballEventView("api-1")).willReturn(createResponse(viewResult));
        given(matchLineupRepository.findByIdIn(List.of(1L))).willReturn(List.of(lineupDocument));
        given(normalizer.normalize(viewResult)).willReturn(FootballDetail.empty());

        synchronizer.sync(SportCode.FOOTBALL, List.of(match));

        verify(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
        verify(matchLineupGoalsService).updateLineupGoals(lineupDocument, viewResult);
        verify(inplayRedisService, never()).markSeenAndEnqueue(anyString(), anyString());
    }

    @Test
    @DisplayName("한 경기의 상세 저장이 실패해도 다음 경기를 계속 처리한다.")
    void sync_WhenOneMatchUpdateFails_ThenContinueNextMatch() {
        Match firstMatch = createMatch(1L, 10L, "api-1");
        Match secondMatch = createMatch(2L, 10L, "api-2");
        BetsFootballViewResponse.ViewResult firstResult = createViewResult("api-1", false);
        BetsFootballViewResponse.ViewResult secondResult = createViewResult("api-2", false);
        given(betsApiClient.getFootballEventView("api-1,api-2"))
                .willReturn(createResponse(firstResult, secondResult));
        given(normalizer.normalize(any(BetsFootballViewResponse.ViewResult.class)))
                .willReturn(FootballDetail.empty());
        doAnswer(invocation -> {
            MatchDetailSyncSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.matchId().equals(1L)) {
                throw new RuntimeException("DB Deadlock");
            }
            return null;
        }).when(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));

        synchronizer.sync(SportCode.FOOTBALL, List.of(firstMatch, secondMatch));

        verify(matchDetailService, times(2)).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
    }

    @Test
    @DisplayName("한 경기의 정규화가 실패해도 다음 경기를 계속 처리한다.")
    void sync_WhenOneMatchNormalizationFails_ThenContinueNextMatch() {
        Match firstMatch = createMatch(1L, 10L, "api-1");
        Match secondMatch = createMatch(2L, 10L, "api-2");
        BetsFootballViewResponse.ViewResult firstResult = createViewResult("api-1", false);
        BetsFootballViewResponse.ViewResult secondResult = createViewResult("api-2", false);
        given(betsApiClient.getFootballEventView("api-1,api-2"))
                .willReturn(createResponse(firstResult, secondResult));
        given(normalizer.normalize(firstResult)).willThrow(new RuntimeException("잘못된 응답"));
        given(normalizer.normalize(secondResult)).willReturn(FootballDetail.empty());

        synchronizer.sync(SportCode.FOOTBALL, List.of(firstMatch, secondMatch));

        verify(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
    }

    private Match createMatch(Long id, Long sportId, String apiMatchId) {
        return Match.builder()
                .id(id)
                .sportId(sportId)
                .apiMatchId(apiMatchId)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
    }

    private List<Match> createMatches(int count) {
        List<Match> matches = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            matches.add(createMatch((long) i, 10L, "api-" + i));
        }
        return matches;
    }

    private BetsFootballViewResponse.ViewResult createViewResult(String id, boolean hasLineup) {
        BetsFootballViewResponse.ViewResult viewResult = new BetsFootballViewResponse.ViewResult();
        viewResult.setId(id);
        viewResult.setSportId("1");
        viewResult.setTimeStatus("1");
        viewResult.setSs("1-0");
        viewResult.setHasLineup(hasLineup ? 1 : 0);
        return viewResult;
    }

    private BetsFootballViewResponse createResponse(BetsFootballViewResponse.ViewResult... results) {
        BetsFootballViewResponse response = new BetsFootballViewResponse();
        response.setResults(List.of(results));
        return response;
    }
}
