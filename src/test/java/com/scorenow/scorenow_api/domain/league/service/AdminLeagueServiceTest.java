package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;
import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;
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

    @InjectMocks
    private AdminLeagueService adminLeagueService;

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
        then(leagueExternalMappingRepository).should(never()).save(any());
    }

    @Test
    void 외부_API_연동_리그를_생성하고_매핑을_저장한다() {
        givenSportExists();
        givenLeagueSaveReturnsArgument();

        AdminLeagueCreateRequest request = externalCreateRequest(TRUE);

        AdminLeagueResponse savedLeague = adminLeagueService.createLeague(request);

        assertThat(savedLeague.getDataOrigin()).isEqualTo(BETS);

        ArgumentCaptor<LeagueExternalMapping> captor = ArgumentCaptor.forClass(LeagueExternalMapping.class);
        then(leagueExternalMappingRepository).should().save(captor.capture());

        LeagueExternalMapping mapping = captor.getValue();
        assertThat(mapping.getDataOrigin()).isEqualTo(BETS);
        assertThat(mapping.getApiLeagueId()).isEqualTo("api_league_id");
        assertThat(mapping.getSyncEnabled()).isTrue();
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
