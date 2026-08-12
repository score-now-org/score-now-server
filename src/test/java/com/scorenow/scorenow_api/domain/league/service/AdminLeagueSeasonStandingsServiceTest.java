package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.GroupMapping;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsGroupMappingsUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsTypeUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsGroupMappingsResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.LeagueSeasonStandingsSyncService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminLeagueSeasonStandingsServiceTest {

    @Mock
    private LeagueSeasonRepository leagueSeasonRepository;

    @Mock
    private LeagueExternalMappingRepository leagueExternalMappingRepository;

    @Mock
    private LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;

    @Mock
    private LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private LeagueSeasonStandingsSyncService leagueSeasonStandingsSyncService;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private AdminLeagueSeasonStandingsService service;

    @Test
    void 이미지_타입_리그_시즌_순위_관리를_등록한다() {
        LeagueSeason leagueSeason = season(10L, manualLeague(1L));
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(leagueSeason));
        given(leagueSeasonStandingsRepository.existsByLeagueSeasonId(10L)).willReturn(false);

        service.createLeagueSeasonStandings(createRequest(10L, LeagueSeasonStandingsType.IMAGE));

        ArgumentCaptor<LeagueSeasonStandings> captor = ArgumentCaptor.forClass(LeagueSeasonStandings.class);
        then(leagueSeasonStandingsRepository).should().save(captor.capture());
        assertThat(captor.getValue().getLeagueSeasonId()).isEqualTo(10L);
        assertThat(captor.getValue().getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
    }

    @Test
    void 비활성_또는_존재하지_않는_시즌이면_순위_관리_등록에_실패한다() {
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLeagueSeasonStandings(
                createRequest(10L, LeagueSeasonStandingsType.IMAGE)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonStandingsRepository).should(never()).save(any());
    }

    @Test
    void 이미_등록된_시즌이면_순위_관리_등록에_실패한다() {
        LeagueSeason leagueSeason = season(10L, manualLeague(1L));
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(leagueSeason));
        given(leagueSeasonStandingsRepository.existsByLeagueSeasonId(10L)).willReturn(true);

        assertThatThrownBy(() -> service.createLeagueSeasonStandings(
                createRequest(10L, LeagueSeasonStandingsType.IMAGE)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LEAGUE_ALREADY_EXISTS);

        then(leagueSeasonStandingsRepository).should(never()).save(any());
    }

    @Test
    void 외부_데이터_타입인데_외부_매핑이_없으면_등록에_실패한다() {
        LeagueSeason leagueSeason = season(10L, betsLeague(1L));
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(leagueSeason));
        given(leagueSeasonStandingsRepository.existsByLeagueSeasonId(10L)).willReturn(false);
        given(leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(DataOrigin.BETS, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLeagueSeasonStandings(
                createRequest(10L, LeagueSeasonStandingsType.EXTERNAL_DATA)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonStandingsRepository).should(never()).save(any());
    }

    @Test
    void 외부_데이터_타입_리그_시즌_순위_관리를_등록한다() {
        LeagueSeason leagueSeason = season(10L, betsLeague(1L));
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(leagueSeason));
        given(leagueSeasonStandingsRepository.existsByLeagueSeasonId(10L)).willReturn(false);
        given(leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(DataOrigin.BETS, 1L))
                .willReturn(Optional.of(LeagueExternalMapping.of(DataOrigin.BETS, "94", 1L, true)));

        service.createLeagueSeasonStandings(createRequest(10L, LeagueSeasonStandingsType.EXTERNAL_DATA));

        then(leagueSeasonStandingsRepository).should().save(any(LeagueSeasonStandings.class));
    }

    @Test
    void 이미지_타입_순위_이미지를_업로드한다() {
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE);
        MultipartFile image = mock(MultipartFile.class);
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L)).willReturn(Optional.of(standings));
        given(fileStorage.uploadFile(image)).willReturn("https://cdn.example.com/standing.png");

        service.uploadLeagueSeasonStandingsImage(10L, image);

        assertThat(standings.getImageUrl()).isEqualTo("https://cdn.example.com/standing.png");
    }

    @Test
    void 이미지_타입이_아니면_순위_이미지_업로드에_실패한다() {
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(standings(10L, LeagueSeasonStandingsType.EXTERNAL_DATA)));

        assertThatThrownBy(() -> service.uploadLeagueSeasonStandingsImage(10L, mock(MultipartFile.class)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(fileStorage).should(never()).uploadFile(any());
    }

    @Test
    void 이미지_타입을_외부_데이터_타입으로_변경한다() {
        LeagueSeason leagueSeason = season(10L, betsLeague(1L));
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE, leagueSeason);
        standings.updateImageUrl("https://cdn.example.com/standing.png");

        given(leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(10L))
                .willReturn(Optional.of(standings));
        given(leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(DataOrigin.BETS, 1L))
                .willReturn(Optional.of(LeagueExternalMapping.of(DataOrigin.BETS, "94", 1L, true)));

        service.updateLeagueSeasonStandingsType(
                10L,
                updateTypeRequest(LeagueSeasonStandingsType.EXTERNAL_DATA)
        );

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.EXTERNAL_DATA);
        assertThat(standings.getImageUrl()).isEqualTo("https://cdn.example.com/standing.png");
        then(leagueSeasonStandingsMongoRepository).should(never()).deleteByLeagueSeasonId(any());
    }

    @Test
    void 외부_데이터_타입을_이미지_타입으로_변경한다() {
        LeagueSeason leagueSeason = season(10L, betsLeague(1L));
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.EXTERNAL_DATA, leagueSeason);

        given(leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(10L))
                .willReturn(Optional.of(standings));

        service.updateLeagueSeasonStandingsType(
                10L,
                updateTypeRequest(LeagueSeasonStandingsType.IMAGE)
        );

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
        then(leagueExternalMappingRepository).should(never()).findByDataOriginAndInternalLeagueId(any(), any());
        then(leagueSeasonStandingsMongoRepository).should(never()).deleteByLeagueSeasonId(any());
    }

    @Test
    void 동일한_타입으로_변경하면_아무것도_하지_않는다() {
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE);
        given(leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(10L))
                .willReturn(Optional.of(standings));

        service.updateLeagueSeasonStandingsType(
                10L,
                updateTypeRequest(LeagueSeasonStandingsType.IMAGE)
        );

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
        then(leagueExternalMappingRepository).should(never()).findByDataOriginAndInternalLeagueId(any(), any());
        then(leagueSeasonStandingsMongoRepository).should(never()).deleteByLeagueSeasonId(any());
    }

    @Test
    void 수동_관리_리그는_외부_데이터_타입으로_변경할_수_없다() {
        LeagueSeason leagueSeason = season(10L, manualLeague(1L));
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE, leagueSeason);

        given(leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(10L))
                .willReturn(Optional.of(standings));

        assertThatThrownBy(() -> service.updateLeagueSeasonStandingsType(
                10L,
                updateTypeRequest(LeagueSeasonStandingsType.EXTERNAL_DATA)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
        then(leagueExternalMappingRepository).should(never()).findByDataOriginAndInternalLeagueId(any(), any());
    }

    @Test
    void 외부_데이터_타입으로_변경할때_외부_매핑이_없으면_실패한다() {
        LeagueSeason leagueSeason = season(10L, betsLeague(1L));
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE, leagueSeason);

        given(leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(10L))
                .willReturn(Optional.of(standings));
        given(leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(DataOrigin.BETS, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLeagueSeasonStandingsType(
                10L,
                updateTypeRequest(LeagueSeasonStandingsType.EXTERNAL_DATA)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
    }

    @Test
    void 외부_데이터_타입이면_수동_동기화를_호출한다() {
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(standings(10L, LeagueSeasonStandingsType.EXTERNAL_DATA)));

        service.syncLeagueSeasonStandingsData(10L);

        then(leagueSeasonStandingsSyncService).should().sync(10L);
    }

    @Test
    void 이미지_타입이면_수동_동기화에_실패한다() {
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(standings(10L, LeagueSeasonStandingsType.IMAGE)));

        assertThatThrownBy(() -> service.syncLeagueSeasonStandingsData(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonStandingsSyncService).should(never()).sync(any());
    }

    @Test
    void 외부_데이터_타입_순위_관리를_삭제하면_Mongo_문서도_삭제한다() {
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.EXTERNAL_DATA);
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L)).willReturn(Optional.of(standings));

        service.deleteLeagueSeasonStandings(10L);

        then(leagueSeasonStandingsMongoRepository).should().deleteByLeagueSeasonId(10L);
        then(leagueSeasonStandingsRepository).should().delete(standings);
    }

    @Test
    void 이미지_타입_순위_관리를_삭제해도_Mongo_문서를_삭제한다() {
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE);
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L)).willReturn(Optional.of(standings));

        service.deleteLeagueSeasonStandings(10L);

        then(leagueSeasonStandingsMongoRepository).should().deleteByLeagueSeasonId(10L);
        then(leagueSeasonStandingsRepository).should().delete(standings);
    }

    @Test
    void 순위_그룹_표시_설정을_조회한다() {
        LeagueSeasonStandingsDataDocument document = standingDataDocument(
                List.of(
                        groupMapping("groupname:western conference", "서부", 1),
                        groupMapping("groupname:eastern conference", "동부", 0)
                ),
                List.of(
                        standingsGroup("groupname:western conference", "MLS 2026, Western Conference", "Western Conference"),
                        standingsGroup("groupname:eastern conference", "MLS 2026, Eastern Conference", "Eastern Conference")
                )
        );
        given(leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(document));

        AdminLeagueSeasonStandingsGroupMappingsResponse response = service.getGroupMappings(10L);

        assertThat(response.getLeagueSeasonId()).isEqualTo(10L);
        assertThat(response.getGroups())
                .extracting(
                        AdminLeagueSeasonStandingsGroupMappingsResponse.Group::getGroupKey,
                        AdminLeagueSeasonStandingsGroupMappingsResponse.Group::getDisplayName,
                        AdminLeagueSeasonStandingsGroupMappingsResponse.Group::getDisplayOrder
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("groupname:eastern conference", "동부", 0),
                        org.assertj.core.groups.Tuple.tuple("groupname:western conference", "서부", 1)
                );
    }

    @Test
    void 순위_그룹_표시명과_표시순서를_수정한다() {
        LeagueSeasonStandingsDataDocument document = standingDataDocument(
                List.of(
                        groupMapping("groupname:western conference", null, null),
                        groupMapping("groupname:eastern conference", null, null)
                ),
                List.of(
                        standingsGroup("groupname:western conference", "West", "Western Conference"),
                        standingsGroup("groupname:eastern conference", "East", "Eastern Conference")
                )
        );
        LeagueSeasonStandingsGroupMappingsUpdateRequest request = groupMappingsRequest(List.of(
                updateGroup("groupname:western conference", " 서부 ", 1),
                updateGroup("groupname:eastern conference", "동부", 0)
        ));

        given(leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(document));

        service.updateGroupMappings(10L, request);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        then(mongoTemplate).should().updateFirst(
                queryCaptor.capture(),
                updateCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(LeagueSeasonStandingsDataDocument.class)
        );

        assertThat(queryCaptor.getValue().getQueryObject().get("leagueSeasonId")).isEqualTo(10L);

        Document setValues = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(setValues).containsOnlyKeys("groupMappings");

        @SuppressWarnings("unchecked")
        List<GroupMapping> updatedMappings = (List<GroupMapping>) setValues.get("groupMappings");
        assertThat(updatedMappings)
                .extracting(GroupMapping::getGroupKey, GroupMapping::getDisplayName, GroupMapping::getDisplayOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("groupname:eastern conference", "동부", 0),
                        org.assertj.core.groups.Tuple.tuple("groupname:western conference", "서부", 1)
                );
    }

    @Test
    void 요청한_그룹_key가_기존_구성과_다르면_수정에_실패한다() {
        LeagueSeasonStandingsDataDocument document = standingDataDocument(
                List.of(groupMapping("groupname:western conference", "서부", 0)),
                List.of(standingsGroup("groupname:western conference", "West", "Western Conference"))
        );
        LeagueSeasonStandingsGroupMappingsUpdateRequest request = groupMappingsRequest(List.of(
                updateGroup("groupname:west division", "서부", 0)
        ));
        given(leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(document));

        assertThatThrownBy(() -> service.updateGroupMappings(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mongoTemplate).shouldHaveNoInteractions();
    }

    @Test
    void 표시순서가_0부터_연속되지_않으면_수정에_실패한다() {
        LeagueSeasonStandingsDataDocument document = standingDataDocument(
                List.of(
                        groupMapping("groupname:western conference", "서부", 0),
                        groupMapping("groupname:eastern conference", "동부", 1)
                ),
                List.of(
                        standingsGroup("groupname:western conference", "West", "Western Conference"),
                        standingsGroup("groupname:eastern conference", "East", "Eastern Conference")
                )
        );
        LeagueSeasonStandingsGroupMappingsUpdateRequest request = groupMappingsRequest(List.of(
                updateGroup("groupname:western conference", "서부", 0),
                updateGroup("groupname:eastern conference", "동부", 2)
        ));
        given(leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(10L))
                .willReturn(Optional.of(document));

        assertThatThrownBy(() -> service.updateGroupMappings(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(mongoTemplate).shouldHaveNoInteractions();
    }

    private LeagueSeasonStandingsCreateRequest createRequest(
            Long leagueSeasonId,
            LeagueSeasonStandingsType standingsType
    ) {
        LeagueSeasonStandingsCreateRequest request = mock(LeagueSeasonStandingsCreateRequest.class);
        given(request.getLeagueSeasonId()).willReturn(leagueSeasonId);
        given(request.getStandingsType()).willReturn(standingsType);
        return request;
    }

    private LeagueSeasonStandingsTypeUpdateRequest updateTypeRequest(LeagueSeasonStandingsType standingsType) {
        return LeagueSeasonStandingsTypeUpdateRequest.builder()
                .standingsType(standingsType)
                .build();
    }

    private LeagueSeason season(Long id, League league) {
        LeagueSeason leagueSeason = LeagueSeason.create(
                league.getId(),
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        );
        ReflectionTestUtils.setField(leagueSeason, "id", id);
        ReflectionTestUtils.setField(leagueSeason, "league", league);
        return leagueSeason;
    }

    private LeagueSeasonStandings standings(Long leagueSeasonId, LeagueSeasonStandingsType type) {
        return LeagueSeasonStandings.builder()
                .leagueSeasonId(leagueSeasonId)
                .standingsType(type)
                .build();
    }

    private LeagueSeasonStandings standings(
            Long leagueSeasonId,
            LeagueSeasonStandingsType type,
            LeagueSeason leagueSeason
    ) {
        return LeagueSeasonStandings.builder()
                .leagueSeasonId(leagueSeasonId)
                .standingsType(type)
                .leagueSeason(leagueSeason)
                .build();
    }

    private LeagueSeasonStandingsDataDocument standingDataDocument(
            List<GroupMapping> groupMappings,
            List<FootballStandingsData.StandingsGroup> groups
    ) {
        return LeagueSeasonStandingsDataDocument.builder()
                .leagueSeasonId(10L)
                .data(FootballStandingsData.builder().groups(groups).build())
                .groupMappings(groupMappings)
                .build();
    }

    private GroupMapping groupMapping(String groupKey, String displayName, Integer displayOrder) {
        return GroupMapping.builder()
                .groupKey(groupKey)
                .displayName(displayName)
                .displayOrder(displayOrder)
                .build();
    }

    private FootballStandingsData.StandingsGroup standingsGroup(
            String groupKey,
            String externalName,
            String externalGroupName
    ) {
        return FootballStandingsData.StandingsGroup.builder()
                .groupKey(groupKey)
                .externalName(externalName)
                .externalGroupName(externalGroupName)
                .build();
    }

    private LeagueSeasonStandingsGroupMappingsUpdateRequest groupMappingsRequest(
            List<LeagueSeasonStandingsGroupMappingsUpdateRequest.Group> groups
    ) {
        return LeagueSeasonStandingsGroupMappingsUpdateRequest.builder()
                .groups(groups)
                .build();
    }

    private LeagueSeasonStandingsGroupMappingsUpdateRequest.Group updateGroup(
            String groupKey,
            String displayName,
            Integer displayOrder
    ) {
        return LeagueSeasonStandingsGroupMappingsUpdateRequest.Group.builder()
                .groupKey(groupKey)
                .displayName(displayName)
                .displayOrder(displayOrder)
                .build();
    }

    private League manualLeague(Long id) {
        return League.builder()
                .id(id)
                .kName("수동 리그")
                .dataOrigin(DataOrigin.MANUAL)
                .build();
    }

    private League betsLeague(Long id) {
        return League.builder()
                .id(id)
                .kName("외부 리그")
                .dataOrigin(DataOrigin.BETS)
                .build();
    }
}
