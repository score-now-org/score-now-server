package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.league.mapper.LeagueSeasonStandingsMapper;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LeagueSeasonStandingsSyncServiceTest {

    @Mock
    private LeagueSeasonStandingsSyncTargetReader syncTargetReader;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private LeagueSeasonStandingsMapper mapper;

    @InjectMocks
    private LeagueSeasonStandingsSyncService syncService;

    @Test
    void 응답이_null이면_동기화에_실패한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(null);

        assertThatThrownBy(() -> syncService.sync(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mapper).should(never()).toDocument(any(), any());
        then(mongoTemplate).should(never()).upsert(any(Query.class), any(Update.class), eq(LeagueSeasonStandingsDataDocument.class));
    }

    @Test
    void success가_1이_아니면_동기화에_실패한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse response = response(0, List.of(new BetsStandingsResponse.Result()));
        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(response);

        assertThatThrownBy(() -> syncService.sync(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mapper).should(never()).toDocument(any(), any());
        then(mongoTemplate).should(never()).upsert(any(Query.class), any(Update.class), eq(LeagueSeasonStandingsDataDocument.class));
    }

    @Test
    void 결과가_없으면_Mongo에_저장하지_않는다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse response = response(1, List.of());
        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(response);

        syncService.sync(10L);

        then(mapper).should(never()).toDocument(any(), any());
        then(mongoTemplate).should(never()).upsert(any(Query.class), any(Update.class), eq(LeagueSeasonStandingsDataDocument.class));
    }

    @Test
    void 결과가_null_요소만_있으면_동기화에_실패한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse response = response(1, Collections.singletonList(null));
        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(response);

        assertThatThrownBy(() -> syncService.sync(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mapper).should(never()).toDocument(any(), any());
        then(mongoTemplate).should(never()).upsert(any(Query.class), any(Update.class), eq(LeagueSeasonStandingsDataDocument.class));
    }

    @Test
    void 첫번째_결과가_null이면_다음_유효한_결과로_동기화한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();
        BetsStandingsResponse response = response(1, Arrays.asList(null, result));
        LeagueSeasonStandingsDataDocument document = LeagueSeasonStandingsDataDocument.builder()
                .leagueId(1L)
                .apiLeagueId("94")
                .dataOrigin(DataOrigin.BETS)
                .leagueSeasonId(10L)
                .externalSeasonName("2025/26")
                .build();

        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(response);
        given(mapper.toDocument(result, target)).willReturn(document);

        syncService.sync(10L);

        then(mapper).should().toDocument(result, target);
        then(mongoTemplate).should().upsert(any(Query.class), any(Update.class), eq(LeagueSeasonStandingsDataDocument.class));
    }

    @Test
    void 정상_응답이면_문서로_변환하고_Mongo에_upsert한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();
        BetsStandingsResponse response = response(1, List.of(result));
        LeagueSeasonStandingsDataDocument document = LeagueSeasonStandingsDataDocument.builder()
                .leagueId(1L)
                .apiLeagueId("94")
                .dataOrigin(DataOrigin.BETS)
                .leagueSeasonId(10L)
                .externalSeasonName("2025/26")
                .build();

        given(syncTargetReader.read(10L)).willReturn(target);
        given(betsApiClient.getStandings("94")).willReturn(response);
        given(mapper.toDocument(result, target)).willReturn(document);

        syncService.sync(10L);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        then(mongoTemplate).should().upsert(
                queryCaptor.capture(),
                updateCaptor.capture(),
                eq(LeagueSeasonStandingsDataDocument.class)
        );

        Document query = queryCaptor.getValue().getQueryObject();
        Document setValues = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);

        assertThat(query.get("leagueSeasonId")).isEqualTo(10L);
        assertThat(setValues).containsKeys(
                "leagueId",
                "apiLeagueId",
                "dataOrigin",
                "leagueSeasonId",
                "externalSeasonName",
                "externalSeasonStartAt",
                "externalSeasonEndAt",
                "standingsTable"
        );
        assertThat(setValues.get("leagueSeasonId")).isEqualTo(10L);
        assertThat(setValues.get("externalSeasonName")).isEqualTo("2025/26");
    }

    private LeagueSeasonStandingsSyncTarget syncTarget() {
        return LeagueSeasonStandingsSyncTarget.builder()
                .leagueSeasonStandingsId(100L)
                .leagueSeasonId(10L)
                .leagueId(1L)
                .dataOrigin(DataOrigin.BETS)
                .apiLeagueId("94")
                .build();
    }

    private BetsStandingsResponse response(Integer success, List<BetsStandingsResponse.Result> results) {
        BetsStandingsResponse response = new BetsStandingsResponse();
        response.setSuccess(success);
        response.setResults(results);
        return response;
    }
}
