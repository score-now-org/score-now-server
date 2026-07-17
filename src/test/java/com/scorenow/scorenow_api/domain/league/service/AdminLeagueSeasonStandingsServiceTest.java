package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    void 이미지_타입_순위_관리를_삭제하면_Mongo_문서는_삭제하지_않는다() {
        LeagueSeasonStandings standings = standings(10L, LeagueSeasonStandingsType.IMAGE);
        given(leagueSeasonStandingsRepository.findByLeagueSeasonId(10L)).willReturn(Optional.of(standings));

        service.deleteLeagueSeasonStandings(10L);

        then(leagueSeasonStandingsMongoRepository).should(never()).deleteByLeagueSeasonId(any());
        then(leagueSeasonStandingsRepository).should().delete(standings);
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
