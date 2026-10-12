package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.BaseballDetailNormalizer;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsBaseballViewResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseballInplayMatchDetailSynchronizerTest {

    @InjectMocks
    private BaseballInplayMatchDetailSynchronizer synchronizer;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private MatchDetailService matchDetailService;

    @Mock
    private BaseballDetailNormalizer normalizer;

    @Test
    void 경기가_10개를_초과하면_View_API를_10개_단위로_호출한다() {
        List<Match> matches = createMatches(12);
        given(betsApiClient.getBaseballEventView(anyString())).willReturn(response());

        synchronizer.sync(SportCode.BASEBALL, matches);

        verify(betsApiClient, times(2)).getBaseballEventView(anyString());
        verify(betsApiClient).getBaseballEventView(
                "api-1,api-2,api-3,api-4,api-5,api-6,api-7,api-8,api-9,api-10");
        verify(betsApiClient).getBaseballEventView("api-11,api-12");
    }

    @Test
    void 일부_API_배치가_실패해도_다음_배치를_계속_호출한다() {
        List<Match> matches = createMatches(12);
        String firstBatch = "api-1,api-2,api-3,api-4,api-5,api-6,api-7,api-8,api-9,api-10";
        String secondBatch = "api-11,api-12";
        given(betsApiClient.getBaseballEventView(firstBatch))
                .willThrow(new RuntimeException("외부 API 호출 실패"));
        given(betsApiClient.getBaseballEventView(secondBatch)).willReturn(response());

        synchronizer.sync(SportCode.BASEBALL, matches);

        verify(betsApiClient).getBaseballEventView(firstBatch);
        verify(betsApiClient).getBaseballEventView(secondBatch);
    }

    @Test
    void View_API_응답에_경기_상세가_없으면_해당_경기를_건너뛴다() {
        Match match = match(1L, "api-1");
        given(betsApiClient.getBaseballEventView("api-1")).willReturn(response());

        synchronizer.sync(SportCode.BASEBALL, List.of(match));

        verifyNoInteractions(normalizer, matchDetailService);
    }

    @Test
    void View_API_응답을_정규화하여_경기_상세_동기화_스냅샷으로_전달한다() {
        Match match = match(1L, "api-1");
        BetsBaseballViewResponse.ViewResult viewResult = viewResult("api-1");
        BaseballDetail normalizedDetail = BaseballDetail.empty();
        given(betsApiClient.getBaseballEventView("api-1")).willReturn(response(viewResult));
        given(normalizer.normalize(viewResult)).willReturn(normalizedDetail);

        synchronizer.sync(SportCode.BASEBALL, List.of(match));

        ArgumentCaptor<MatchDetailSyncSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(MatchDetailSyncSnapshot.class);
        verify(matchDetailService).updateInplayMatchDetail(snapshotCaptor.capture());

        MatchDetailSyncSnapshot snapshot = snapshotCaptor.getValue();
        assertThat(snapshot.dataOrigin()).isEqualTo(DataOrigin.BETS);
        assertThat(snapshot.sportCode()).isEqualTo(SportCode.BASEBALL);
        assertThat(snapshot.matchId()).isEqualTo(1L);
        assertThat(snapshot.externalSportId()).isEqualTo("3");
        assertThat(snapshot.homeScore()).isEqualTo(1);
        assertThat(snapshot.awayScore()).isEqualTo(4);
        assertThat(snapshot.status()).isEqualTo(MatchStatus.IN_PLAY);
        assertThat(snapshot.stadium().externalId()).isEqualTo("stadium-1");
        assertThat(snapshot.stadium().name()).isEqualTo("Baseball Stadium");
        assertThat(snapshot.stadium().city()).isEqualTo("Seoul");
        assertThat(snapshot.sportDetail()).isSameAs(normalizedDetail);
    }

    @Test
    void 동일한_경기_ID가_응답에_중복되면_먼저_수신한_결과를_사용한다() {
        Match match = match(1L, "api-1");
        BetsBaseballViewResponse.ViewResult first = viewResult("api-1");
        BetsBaseballViewResponse.ViewResult duplicate = viewResult("api-1");
        first.setTime("first");
        duplicate.setTime("duplicate");
        BaseballDetail normalizedDetail = BaseballDetail.empty();
        given(betsApiClient.getBaseballEventView("api-1")).willReturn(response(first, duplicate));
        given(normalizer.normalize(first)).willReturn(normalizedDetail);

        synchronizer.sync(SportCode.BASEBALL, List.of(match));

        verify(normalizer).normalize(first);
        verify(normalizer, never()).normalize(duplicate);
        verify(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
    }

    @Test
    void 한_경기의_정규화가_실패해도_다음_경기를_계속_처리한다() {
        Match firstMatch = match(1L, "api-1");
        Match secondMatch = match(2L, "api-2");
        BetsBaseballViewResponse.ViewResult firstResult = viewResult("api-1");
        BetsBaseballViewResponse.ViewResult secondResult = viewResult("api-2");
        given(betsApiClient.getBaseballEventView("api-1,api-2"))
                .willReturn(response(firstResult, secondResult));
        given(normalizer.normalize(firstResult)).willThrow(new RuntimeException("정규화 실패"));
        given(normalizer.normalize(secondResult)).willReturn(BaseballDetail.empty());

        synchronizer.sync(SportCode.BASEBALL, List.of(firstMatch, secondMatch));

        verify(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
    }

    @Test
    void 한_경기의_상세_저장이_실패해도_다음_경기를_계속_처리한다() {
        Match firstMatch = match(1L, "api-1");
        Match secondMatch = match(2L, "api-2");
        BetsBaseballViewResponse.ViewResult firstResult = viewResult("api-1");
        BetsBaseballViewResponse.ViewResult secondResult = viewResult("api-2");
        given(betsApiClient.getBaseballEventView("api-1,api-2"))
                .willReturn(response(firstResult, secondResult));
        given(normalizer.normalize(any(BetsBaseballViewResponse.ViewResult.class)))
                .willReturn(BaseballDetail.empty());
        doAnswer(invocation -> {
            MatchDetailSyncSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.matchId().equals(1L)) {
                throw new RuntimeException("상세 저장 실패");
            }
            return null;
        }).when(matchDetailService).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));

        synchronizer.sync(SportCode.BASEBALL, List.of(firstMatch, secondMatch));

        verify(matchDetailService, times(2)).updateInplayMatchDetail(any(MatchDetailSyncSnapshot.class));
    }

    private Match match(Long id, String apiMatchId) {
        return Match.builder()
                .id(id)
                .sportId(20L)
                .apiMatchId(apiMatchId)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
    }

    private List<Match> createMatches(int count) {
        List<Match> matches = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            matches.add(match((long) index, "api-" + index));
        }
        return matches;
    }

    private BetsBaseballViewResponse.ViewResult viewResult(String id) {
        BetsBaseballViewResponse.Score runScore = new BetsBaseballViewResponse.Score();
        runScore.setHome("1");
        runScore.setAway("4");

        BetsBaseballViewResponse.StadiumData stadiumData = new BetsBaseballViewResponse.StadiumData();
        stadiumData.setId("stadium-1");
        stadiumData.setName("Baseball Stadium");
        stadiumData.setCity("Seoul");

        BetsBaseballViewResponse.Extra extra = new BetsBaseballViewResponse.Extra();
        extra.setStadiumData(stadiumData);

        BetsBaseballViewResponse.ViewResult viewResult = new BetsBaseballViewResponse.ViewResult();
        viewResult.setId(id);
        viewResult.setSportId("3");
        viewResult.setTimeStatus("1");
        viewResult.setScores(Map.of("run", runScore));
        viewResult.setExtra(extra);
        return viewResult;
    }

    private BetsBaseballViewResponse response(BetsBaseballViewResponse.ViewResult... results) {
        BetsBaseballViewResponse response = new BetsBaseballViewResponse();
        response.setResults(List.of(results));
        return response;
    }
}
