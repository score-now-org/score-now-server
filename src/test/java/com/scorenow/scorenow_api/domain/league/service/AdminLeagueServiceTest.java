package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
import static com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder.HOME_AWAY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminLeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private LeagueExternalMappingRepository leagueExternalMappingRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private AdminLeagueService adminLeagueService;

    @Test
    void 리그_관리_옵션을_조회한다() {
        Sport sport = Sport.builder()
                .id(1L)
                .kName("축구")
                .build();
        given(sportRepository.findAll()).willReturn(List.of(sport));

        AdminLeagueSearchOptionsResponse result = adminLeagueService.getSearchOptions();

        assertThat(result.getSports()).hasSize(1);
        assertThat(result.getSports().get(0).getId()).isEqualTo(1L);
        assertThat(result.getSports().get(0).getLabel()).isEqualTo("축구");
        assertThat(result.getTeamDisplayOrders())
                .extracting(AdminLeagueSearchOptionsResponse.EnumOption::getValue)
                .containsExactly("HOME_AWAY", "AWAY_HOME");
        assertThat(result.getDataOrigins())
                .extracting(AdminLeagueSearchOptionsResponse.EnumOption::getValue)
                .containsExactly("BETS")
                .doesNotContain("MANUAL");
    }

    @Test
    void 외부_API_연동_없이_리그를_생성한다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();

        AdminLeagueCreateRequest request = AdminLeagueCreateRequest.builder()
                .sportId(1L)
                .eName("league eName")
                .kName("리그 한글명")
                .externalLinked(FALSE)
                .build();

        AdminLeagueResponse savedLeague = adminLeagueService.createLeague(request);

        assertThat(savedLeague.getDataOrigin()).isEqualTo(MANUAL);
        assertThat(savedLeague.getSportName()).isEqualTo("soccer");
        assertThat(savedLeague.getTeamDisplayOrder()).isEqualTo(HOME_AWAY);
        assertThat(savedLeague.getSyncEnabled()).isNull();
        then(leagueExternalMappingRepository).should(never()).save(any());
    }

    @Test
    void 외부_API_연동_리그를_생성하고_매핑을_저장한다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();

        AdminLeagueCreateRequest request = externalCreateRequest(TRUE);

        AdminLeagueResponse savedLeague = adminLeagueService.createLeague(request);

        assertThat(savedLeague.getDataOrigin()).isEqualTo(BETS);
        assertThat(savedLeague.getSportName()).isEqualTo("soccer");
        assertThat(savedLeague.getTeamDisplayOrder()).isEqualTo(HOME_AWAY);
        assertThat(savedLeague.getSyncEnabled()).isTrue();

        ArgumentCaptor<LeagueExternalMapping> captor = ArgumentCaptor.forClass(LeagueExternalMapping.class);
        then(leagueExternalMappingRepository).should().save(captor.capture());

        LeagueExternalMapping mapping = captor.getValue();
        assertThat(mapping.getDataOrigin()).isEqualTo(BETS);
        assertThat(mapping.getApiLeagueId()).isEqualTo("api_league_id");
        assertThat(mapping.getSyncEnabled()).isTrue();
    }

    @Test
    void 리그와_엠블럼을_함께_등록한다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();
        MockMultipartFile image = emblemFile();
        given(fileStorage.uploadFileAndReturnKey(image, "league-emblems"))
                .willReturn("league-emblems/emblem.png");
        given(fileStorage.getFileUrl("league-emblems/emblem.png"))
                .willReturn("https://storage.googleapis.com/bucket/league-emblems/emblem.png");

        AdminLeagueResponse response = adminLeagueService.createLeague(externalCreateRequest(TRUE), image);

        assertThat(response.getImageUrl())
                .isEqualTo("https://storage.googleapis.com/bucket/league-emblems/emblem.png");
    }

    @Test
    void 트랜잭션이_롤백되면_새로_업로드한_엠블럼을_삭제한다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();
        MockMultipartFile image = emblemFile();
        String imageKey = "league-emblems/emblem.png";
        given(fileStorage.uploadFileAndReturnKey(image, "league-emblems")).willReturn(imageKey);
        given(fileStorage.getFileUrl(imageKey))
                .willReturn("https://storage.googleapis.com/bucket/league-emblems/emblem.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            adminLeagueService.createLeague(externalCreateRequest(TRUE), image);

            TransactionSynchronization synchronization = TransactionSynchronizationManager
                    .getSynchronizations()
                    .get(0);
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            then(fileStorage).should().deleteFile(imageKey);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션_결과를_알_수_없으면_신규_엠블럼을_삭제하지_않는다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();
        MockMultipartFile image = emblemFile();
        String imageKey = "league-emblems/emblem.png";
        given(fileStorage.uploadFileAndReturnKey(image, "league-emblems")).willReturn(imageKey);
        given(fileStorage.getFileUrl(imageKey))
                .willReturn("https://storage.googleapis.com/bucket/league-emblems/emblem.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            adminLeagueService.createLeague(externalCreateRequest(TRUE), image);

            TransactionSynchronization synchronization = TransactionSynchronizationManager
                    .getSynchronizations()
                    .get(0);
            synchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

            then(fileStorage).should(never()).deleteFile(imageKey);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 기존_엠블럼을_새_이미지로_교체한다() {
        Long leagueId = 1L;
        String previousImageUrl = "https://storage.googleapis.com/bucket/league-emblems/old.png";
        League league = League.builder()
                .id(leagueId)
                .sportId(1L)
                .eName("League")
                .kName("리그")
                .dataOrigin(MANUAL)
                .imageUrl(previousImageUrl)
                .build();
        AdminLeagueUpdateRequest request = mock(AdminLeagueUpdateRequest.class);
        given(request.getSportId()).willReturn(null);
        MockMultipartFile image = emblemFile();
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
        given(fileStorage.uploadFileAndReturnKey(image, "league-emblems"))
                .willReturn("league-emblems/new.png");
        given(fileStorage.getFileUrl("league-emblems/new.png"))
                .willReturn("https://storage.googleapis.com/bucket/league-emblems/new.png");
        given(fileStorage.isManagedFileUrl(previousImageUrl)).willReturn(true);

        adminLeagueService.updateLeague(leagueId, request, image);

        assertThat(league.getImageUrl())
                .isEqualTo("https://storage.googleapis.com/bucket/league-emblems/new.png");
        then(fileStorage).should().deleteFileByUrl(previousImageUrl);
    }

    @Test
    void removeEmblem이_true면_기존_엠블럼을_제거한다() {
        Long leagueId = 1L;
        String previousImageUrl = "https://storage.googleapis.com/bucket/league-emblems/old.png";
        League league = League.builder()
                .id(leagueId)
                .sportId(1L)
                .eName("League")
                .kName("리그")
                .dataOrigin(MANUAL)
                .imageUrl(previousImageUrl)
                .build();
        AdminLeagueUpdateRequest request = mock(AdminLeagueUpdateRequest.class);
        given(request.getSportId()).willReturn(null);
        given(request.getRemoveEmblem()).willReturn(TRUE);
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
        given(fileStorage.isManagedFileUrl(previousImageUrl)).willReturn(true);

        adminLeagueService.updateLeague(leagueId, request, null);

        assertThat(league.getImageUrl()).isNull();
        then(fileStorage).should().deleteFileByUrl(previousImageUrl);
    }

    @Test
    void 이미지가_없고_removeEmblem이_false면_기존_엠블럼을_유지한다() {
        Long leagueId = 1L;
        String previousImageUrl = "https://storage.googleapis.com/bucket/league-emblems/old.png";
        League league = League.builder()
                .id(leagueId)
                .sportId(1L)
                .eName("League")
                .kName("리그")
                .dataOrigin(MANUAL)
                .imageUrl(previousImageUrl)
                .build();
        AdminLeagueUpdateRequest request = mock(AdminLeagueUpdateRequest.class);
        given(request.getSportId()).willReturn(null);
        given(request.getRemoveEmblem()).willReturn(false);
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));

        adminLeagueService.updateLeague(leagueId, request, null);

        assertThat(league.getImageUrl()).isEqualTo(previousImageUrl);
        then(fileStorage).should(never()).uploadFileAndReturnKey(any(), any());
        then(fileStorage).should(never()).deleteFileByUrl(any());
    }

    @Test
    void 엠블럼_제거와_새_이미지를_동시에_요청할_수_없다() {
        Long leagueId = 1L;
        League league = createManualLeague(leagueId);
        AdminLeagueUpdateRequest request = mock(AdminLeagueUpdateRequest.class);
        given(request.getRemoveEmblem()).willReturn(TRUE);
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));

        assertThatThrownBy(() -> adminLeagueService.updateLeague(leagueId, request, emblemFile()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(fileStorage).should(never()).uploadFileAndReturnKey(any(), any());
    }

    @Test
    void 외부_API_연동_리그_생성시_dataOrigin이_MANUAL이면_예외가_발생한다() {
        givenSportExists();

        AdminLeagueCreateRequest request = AdminLeagueCreateRequest.builder()
                .sportId(1L)
                .eName("league eName")
                .externalLinked(TRUE)
                .dataOrigin(MANUAL)
                .apiLeagueId("api_league_id")
                .build();

        assertThatThrownBy(() -> adminLeagueService.createLeague(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueRepository).should(never()).save(any());
        then(leagueExternalMappingRepository).should(never()).save(any());
    }

    @Test
    void 리그_ID와_키워드로_검색하고_종목명과_외부_동기화_여부를_응답한다() {
        Sport sport = Sport.builder()
                .id(1L)
                .kName("축구")
                .build();
        League league = League.builder()
                .id(10L)
                .sportId(1L)
                .sport(sport)
                .eName("Premier League")
                .kName("프리미어리그")
                .dataOrigin(BETS)
                .build();
        LeagueSearchCondition condition = new LeagueSearchCondition();
        condition.setLeagueId(10L);
        condition.setKeyword(" 프리미어 ");

        given(leagueRepository.searchLeagues(10L, "프리미어"))
                .willReturn(List.of(league));
        given(leagueExternalMappingRepository.findByInternalLeagueIdIn(List.of(10L)))
                .willReturn(List.of(LeagueExternalMapping.of(BETS, "api_league_id", 10L, TRUE)));

        List<AdminLeagueResponse> result = adminLeagueService.searchLeagues(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getSportName()).isEqualTo("축구");
        assertThat(result.get(0).getSyncEnabled()).isTrue();
        then(leagueRepository).should().searchLeagues(10L, "프리미어");
        then(leagueExternalMappingRepository).should().findByInternalLeagueIdIn(List.of(10L));
    }

    @Test
    void 수동_관리_리그_검색시_외부_동기화_여부는_null로_응답한다() {
        League league = League.builder()
                .id(10L)
                .sportId(1L)
                .eName("Manual League")
                .kName("수동 리그")
                .dataOrigin(MANUAL)
                .build();
        LeagueSearchCondition condition = new LeagueSearchCondition();

        given(leagueRepository.searchLeagues(null, null))
                .willReturn(List.of(league));

        List<AdminLeagueResponse> result = adminLeagueService.searchLeagues(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSyncEnabled()).isNull();
        then(leagueExternalMappingRepository).should(never()).findByInternalLeagueIdIn(any());
    }

    @Test
    void 외부_API_리그_ID를_수정한다() {
        Long leagueId = 1L;
        League league = createExternalLeague(leagueId);
        LeagueExternalMapping mapping = LeagueExternalMapping.of(BETS, "old_api_league_id", leagueId, TRUE);
        givenExternalMapping(leagueId, league, mapping);

        adminLeagueService.updateApiLeagueId(leagueId, apiLeagueIdUpdateRequest("new_api_league_id"));

        assertThat(mapping.getApiLeagueId()).isEqualTo("new_api_league_id");
    }

    @Test
    void 내부_전용_리그의_외부_API_리그_ID를_수정하면_예외가_발생한다() {
        Long leagueId = 1L;
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(createManualLeague(leagueId)));

        assertThatThrownBy(() -> adminLeagueService.updateApiLeagueId(
                leagueId,
                mock(LeagueApiLeagueIdUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueExternalMappingRepository).should(never())
                .findByDataOriginAndInternalLeagueId(any(), any());
    }

    @Test
    void 외부_API_동기화_여부를_true로_변경한다() {
        Long leagueId = 1L;
        LeagueExternalMapping mapping = LeagueExternalMapping.of(BETS, "api_league_id", leagueId, FALSE);
        givenExternalMapping(leagueId, createExternalLeague(leagueId), mapping);

        adminLeagueService.updateSyncEnabled(leagueId, syncEnabledUpdateRequest(TRUE));

        assertThat(mapping.getSyncEnabled()).isTrue();
    }

    @Test
    void 외부_API_동기화_여부를_false로_변경한다() {
        Long leagueId = 1L;
        LeagueExternalMapping mapping = LeagueExternalMapping.of(BETS, "api_league_id", leagueId, TRUE);
        givenExternalMapping(leagueId, createExternalLeague(leagueId), mapping);

        adminLeagueService.updateSyncEnabled(leagueId, syncEnabledUpdateRequest(FALSE));

        assertThat(mapping.getSyncEnabled()).isFalse();
    }

    @Test
    void 내부_전용_리그의_외부_API_동기화_여부를_수정하면_예외가_발생한다() {
        Long leagueId = 1L;
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(createManualLeague(leagueId)));

        assertThatThrownBy(() -> adminLeagueService.updateSyncEnabled(
                leagueId,
                mock(LeagueSyncEnabledUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueExternalMappingRepository).should(never())
                .findByDataOriginAndInternalLeagueId(any(), any());
    }

    private void givenSportExists() {
        Sport sport = Sport.builder()
                .id(1L)
                .kName("soccer")
                .build();
        given(sportRepository.findById(1L)).willReturn(Optional.of(sport));
    }

    private void givenLeagueSaveReturnsArgument() {
        given(leagueRepository.save(any(League.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private AdminLeagueCreateRequest externalCreateRequest(Boolean syncEnabled) {
        return AdminLeagueCreateRequest.builder()
                .sportId(1L)
                .eName("league eName")
                .kName("리그 한글명")
                .externalLinked(TRUE)
                .dataOrigin(BETS)
                .apiLeagueId("api_league_id")
                .syncEnabled(syncEnabled)
                .build();
    }

    private void givenExternalMapping(Long leagueId, League league, LeagueExternalMapping mapping) {
        given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
        given(leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(BETS, leagueId))
                .willReturn(Optional.of(mapping));
    }

    private League createExternalLeague(Long leagueId) {
        return League.builder()
                .id(leagueId)
                .sportId(1L)
                .eName("League")
                .kName("리그")
                .dataOrigin(BETS)
                .build();
    }

    private League createManualLeague(Long leagueId) {
        return League.builder()
                .id(leagueId)
                .sportId(1L)
                .eName("League")
                .kName("리그")
                .dataOrigin(MANUAL)
                .build();
    }

    private MockMultipartFile emblemFile() {
        return new MockMultipartFile(
                "image",
                "emblem.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
    }

    private LeagueApiLeagueIdUpdateRequest apiLeagueIdUpdateRequest(String apiLeagueId) {
        LeagueApiLeagueIdUpdateRequest request = mock(LeagueApiLeagueIdUpdateRequest.class);
        given(request.getApiLeagueId()).willReturn(apiLeagueId);
        return request;
    }

    private LeagueSyncEnabledUpdateRequest syncEnabledUpdateRequest(Boolean syncEnabled) {
        LeagueSyncEnabledUpdateRequest request = mock(LeagueSyncEnabledUpdateRequest.class);
        given(request.getSyncEnabled()).willReturn(syncEnabled);
        return request;
    }
}
