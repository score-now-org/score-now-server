package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.GroupMapping;
import com.scorenow.scorenow_api.domain.league.document.standings.StandingsData;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.LeagueSeasonStandingsSyncService;
import com.scorenow.scorenow_api.domain.league.service.standings.LeagueSeasonStandingsSyncTargetReader;
import com.scorenow.scorenow_api.domain.league.service.standings.normalizer.StandingsNormalizerRegistry;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LeagueSeasonStandingsSyncServiceTest {

    private static final Long LEAGUE_SEASON_ID = 10L;
    private static final String API_LEAGUE_ID = "94";

    @Mock
    private LeagueSeasonStandingsSyncTargetReader syncTargetReader;

    @Mock
    private BetsApiClient betsApiClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private LeagueSeasonStandingsMongoRepository mongoRepository;

    @Mock
    private StandingsNormalizerRegistry normalizerRegistry;

    @InjectMocks
    private LeagueSeasonStandingsSyncService syncService;

    @Test
    void 응답이_null이면_동기화에_실패한다() {
        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(syncTarget());
        given(betsApiClient.getStandings(API_LEAGUE_ID)).willReturn(null);

        assertThatThrownBy(() -> syncService.sync(LEAGUE_SEASON_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(normalizerRegistry).shouldHaveNoInteractions();
        then(mongoRepository).shouldHaveNoInteractions();
        then(mongoTemplate).shouldHaveNoInteractions();
    }

    @Test
    void success가_1이_아니면_동기화에_실패한다() {
        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(syncTarget());
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(0, List.of(new BetsStandingsResponse.Result())));

        assertThatThrownBy(() -> syncService.sync(LEAGUE_SEASON_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(normalizerRegistry).shouldHaveNoInteractions();
        then(mongoRepository).shouldHaveNoInteractions();
        then(mongoTemplate).shouldHaveNoInteractions();
    }

    @Test
    void 결과가_없으면_Mongo에_저장하지_않는다() {
        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(syncTarget());
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, List.of()));

        syncService.sync(LEAGUE_SEASON_ID);

        then(normalizerRegistry).shouldHaveNoInteractions();
        then(mongoRepository).shouldHaveNoInteractions();
        then(mongoTemplate).shouldHaveNoInteractions();
    }

    @Test
    void 결과가_null_요소만_있으면_동기화에_실패한다() {
        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(syncTarget());
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, Collections.singletonList(null)));

        assertThatThrownBy(() -> syncService.sync(LEAGUE_SEASON_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(normalizerRegistry).shouldHaveNoInteractions();
        then(mongoRepository).shouldHaveNoInteractions();
        then(mongoTemplate).shouldHaveNoInteractions();
    }

    @Test
    void 첫번째_결과가_null이면_다음_유효한_결과로_초기화한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result validResult = new BetsStandingsResponse.Result();
        LeagueSeasonStandingsDataDocument newDocument = document(
                List.of(groupMapping("single", null, null))
        );

        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(target);
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, Arrays.asList(null, validResult)));
        given(normalizerRegistry.normalize(validResult, target)).willReturn(newDocument);
        given(mongoRepository.findByLeagueSeasonId(LEAGUE_SEASON_ID))
                .willReturn(Optional.empty());

        syncService.sync(LEAGUE_SEASON_ID);

        then(normalizerRegistry).should().normalize(validResult, target);
        then(mongoTemplate).should().upsert(
                any(Query.class),
                any(Update.class),
                eq(LeagueSeasonStandingsDataDocument.class)
        );
        then(mongoTemplate).should(never()).updateFirst(
                any(Query.class),
                any(Update.class),
                eq(LeagueSeasonStandingsDataDocument.class)
        );
    }

    @Test
    void 최초_동기화이면_순위_데이터와_그룹_매핑을_upsert한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();
        LeagueSeasonStandingsDataDocument newDocument = document(List.of(
                groupMapping("groupname:western conference", null, null),
                groupMapping("groupname:eastern conference", null, null),
                groupMapping("groupname:<null>", null, null)
        ));

        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(target);
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, List.of(result)));
        given(normalizerRegistry.normalize(result, target)).willReturn(newDocument);
        given(mongoRepository.findByLeagueSeasonId(LEAGUE_SEASON_ID))
                .willReturn(Optional.empty());

        syncService.sync(LEAGUE_SEASON_ID);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        then(mongoTemplate).should().upsert(
                queryCaptor.capture(),
                updateCaptor.capture(),
                eq(LeagueSeasonStandingsDataDocument.class)
        );
        then(mongoTemplate).should(never()).updateFirst(
                any(Query.class),
                any(Update.class),
                eq(LeagueSeasonStandingsDataDocument.class)
        );

        Document query = queryCaptor.getValue().getQueryObject();
        Document setValues = setValues(updateCaptor.getValue());

        assertThat(query.get("leagueSeasonId")).isEqualTo(LEAGUE_SEASON_ID);
        assertThat(setValues).containsKeys(
                "sportId",
                "leagueId",
                "apiLeagueId",
                "dataOrigin",
                "leagueSeasonId",
                "leagueSeasonStandingsId",
                "syncedAt",
                "data",
                "externalSeason",
                "groupMappings"
        );
        assertThat(setValues.get("groupMappings"))
                .isEqualTo(newDocument.getGroupMappings());
    }

    @Test
    void 기존_그룹과_신규_그룹의_key가_같으면_순위_데이터만_업데이트한다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();

        LeagueSeasonStandingsDataDocument currentDocument = document(List.of(
                groupMapping("groupname:western conference", "서부", 1),
                groupMapping("groupname:eastern conference", "동부", 0),
                groupMapping("groupname:<null>", "통합", 2)
        ));
        LeagueSeasonStandingsDataDocument newDocument = document(List.of(
                groupMapping("groupname:<null>", null, null),
                groupMapping("groupname:eastern conference", null, null),
                groupMapping("groupname:western conference", null, null)
        ));

        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(target);
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, List.of(result)));
        given(normalizerRegistry.normalize(result, target)).willReturn(newDocument);
        given(mongoRepository.findByLeagueSeasonId(LEAGUE_SEASON_ID))
                .willReturn(Optional.of(currentDocument));

        syncService.sync(LEAGUE_SEASON_ID);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        then(mongoTemplate).should().updateFirst(
                queryCaptor.capture(),
                updateCaptor.capture(),
                eq(LeagueSeasonStandingsDataDocument.class)
        );
        then(mongoTemplate).should(never()).upsert(
                any(Query.class),
                any(Update.class),
                eq(LeagueSeasonStandingsDataDocument.class)
        );

        Document query = queryCaptor.getValue().getQueryObject();
        Document setValues = setValues(updateCaptor.getValue());

        assertThat(query.get("leagueSeasonId")).isEqualTo(LEAGUE_SEASON_ID);
        assertThat(setValues).doesNotContainKey("groupMappings");
        assertThat(setValues.get("data")).isSameAs(newDocument.getData());
    }

    @Test
    void 기존_그룹과_신규_그룹의_key가_다르면_데이터를_업데이트하지_않는다() {
        LeagueSeasonStandingsSyncTarget target = syncTarget();
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();

        LeagueSeasonStandingsDataDocument currentDocument = document(List.of(
                groupMapping("groupname:western conference", "서부", 1),
                groupMapping("groupname:eastern conference", "동부", 0)
        ));
        LeagueSeasonStandingsDataDocument newDocument = document(List.of(
                groupMapping("groupname:west division", null, null),
                groupMapping("groupname:eastern conference", null, null)
        ));

        given(syncTargetReader.read(LEAGUE_SEASON_ID)).willReturn(target);
        given(betsApiClient.getStandings(API_LEAGUE_ID))
                .willReturn(response(1, List.of(result)));
        given(normalizerRegistry.normalize(result, target)).willReturn(newDocument);
        given(mongoRepository.findByLeagueSeasonId(LEAGUE_SEASON_ID))
                .willReturn(Optional.of(currentDocument));

        assertThatThrownBy(() -> syncService.sync(LEAGUE_SEASON_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mongoTemplate).shouldHaveNoInteractions();
    }

    private LeagueSeasonStandingsSyncTarget syncTarget() {
        return LeagueSeasonStandingsSyncTarget.builder()
                .sportId(1L)
                .leagueSeasonStandingsId(100L)
                .leagueSeasonId(LEAGUE_SEASON_ID)
                .leagueId(1L)
                .dataOrigin(DataOrigin.BETS)
                .apiLeagueId(API_LEAGUE_ID)
                .build();
    }

    private LeagueSeasonStandingsDataDocument document(
            List<GroupMapping> groupMappings
    ) {
        return LeagueSeasonStandingsDataDocument.builder()
                .sportId(1L)
                .leagueId(1L)
                .apiLeagueId(API_LEAGUE_ID)
                .dataOrigin(DataOrigin.BETS)
                .leagueSeasonId(LEAGUE_SEASON_ID)
                .leagueSeasonStandingsId(100L)
                .syncedAt(Instant.parse("2026-08-12T00:00:00Z"))
                .data(org.mockito.Mockito.mock(StandingsData.class))
                .externalSeason(LeagueSeasonStandingsDataDocument.ExternalSeason.builder()
                        .name("2026")
                        .startAt(1L)
                        .endAt(2L)
                        .build())
                .groupMappings(groupMappings)
                .build();
    }

    private GroupMapping groupMapping(
            String groupKey,
            String displayName,
            Integer displayOrder
    ) {
        return GroupMapping.builder()
                .groupKey(groupKey)
                .displayName(displayName)
                .displayOrder(displayOrder)
                .build();
    }

    private BetsStandingsResponse response(
            Integer success,
            List<BetsStandingsResponse.Result> results
    ) {
        BetsStandingsResponse response = new BetsStandingsResponse();
        response.setSuccess(success);
        response.setResults(results);
        return response;
    }

    private Document setValues(Update update) {
        return update.getUpdateObject().get("$set", Document.class);
    }
}
