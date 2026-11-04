package com.scorenow.scorenow_api.domain.player.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerCreateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerSquadUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class AdminPlayerServiceTest {

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private LeagueRepository leagueRepository;

	@Mock
	private SportRepository sportRepository;

	@Mock
	private PlayerTeamDetailRepository playerTeamDetailRepository;

	@Mock
	private PlayerExternalMappingRepository playerExternalMappingRepository;

	@InjectMocks
	private AdminPlayerService service;

	// ========================================
	// 1. 선수 등록 성공
	// ========================================

	@ParameterizedTest
	@EnumSource(
		value = DataOrigin.class,
		mode = EnumSource.Mode.EXCLUDE,
		names = "MANUAL"
	)
	void 외부_API_Provider는_API_ID와_외부매핑을_저장한다(
		DataOrigin provider
	) {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(provider, " 12345 ");

		when(request.kName()).thenReturn("손흥민");
		when(request.eName()).thenReturn("Son Heung-min");
		when(request.cc()).thenReturn("KR");
		when(request.birthdate())
			.thenReturn(LocalDate.of(1992, 7, 8));
		when(request.height()).thenReturn(183);
		when(request.shirtNumber()).thenReturn("7");

		when(playerExternalMappingRepository
			.findByProviderAndApiPlayerId(provider, "12345"))
			.thenReturn(Optional.empty());

		Player savedPlayer = mockSavedPlayer(100L);

		Long result = service.createPlayer(request);

		assertThat(result).isEqualTo(100L);

		// 선수 정보 저장 검증
		ArgumentCaptor<Player> playerCaptor =
			ArgumentCaptor.forClass(Player.class);

		verify(playerRepository).save(playerCaptor.capture());

		Player createdPlayer = playerCaptor.getValue();

		assertThat(createdPlayer.getKName())
			.isEqualTo("손흥민");
		assertThat(createdPlayer.getEName())
			.isEqualTo("Son Heung-min");
		assertThat(createdPlayer.getCc())
			.isEqualTo("KR");
		assertThat(createdPlayer.getBirthdate())
			.isEqualTo(LocalDate.of(1992, 7, 8));
		assertThat(createdPlayer.getHeight())
			.isEqualTo(183);
		assertThat(createdPlayer.isTaegeuk())
			.isFalse();

		// Provider + API ID 중복 조회 검증
		verify(playerExternalMappingRepository)
			.findByProviderAndApiPlayerId(provider, "12345");

		// 외부 매핑 저장 검증
		ArgumentCaptor<PlayerExternalMapping> mappingCaptor =
			ArgumentCaptor.forClass(PlayerExternalMapping.class);

		verify(playerExternalMappingRepository)
			.save(mappingCaptor.capture());

		PlayerExternalMapping mapping = mappingCaptor.getValue();

		assertThat(mapping.getProvider())
			.isEqualTo(provider);
		assertThat(mapping.getApiPlayerId())
			.isEqualTo("12345");
		assertThat(mapping.getPlayer())
			.isSameAs(savedPlayer);

		// 팀 소속 정보 저장 검증
		ArgumentCaptor<PlayerTeamDetail> detailCaptor =
			ArgumentCaptor.forClass(PlayerTeamDetail.class);

		verify(playerTeamDetailRepository)
			.save(detailCaptor.capture());

		PlayerTeamDetail detail = detailCaptor.getValue();

		assertThat(detail.getPlayer())
			.isSameAs(savedPlayer);
		assertThat(detail.getShirtNumber())
			.isEqualTo("7");
		assertThat(detail.isSquadOn())
			.isFalse();
	}

	@Test
	void MANUAL_선수는_API_ID없이_등록할_수_있다() {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, null);

		mockSavedPlayer(101L);

		Long result = service.createPlayer(request);

		assertThat(result).isEqualTo(101L);

		verify(playerRepository).save(any(Player.class));

		// MANUAL은 외부 매핑 조회 및 저장 없음
		verifyNoInteractions(playerExternalMappingRepository);

		verify(playerTeamDetailRepository)
			.save(any(PlayerTeamDetail.class));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "   "})
	void MANUAL_선수의_API_ID가_공백이면_등록할_수_있다(
		String apiPlayerId
	) {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, apiPlayerId);

		mockSavedPlayer(102L);

		Long result = service.createPlayer(request);

		assertThat(result).isEqualTo(102L);

		verify(playerRepository).save(any(Player.class));

		verifyNoInteractions(playerExternalMappingRepository);

		verify(playerTeamDetailRepository)
			.save(any(PlayerTeamDetail.class));
	}

	@Test
	void 리그가_있으면_팀소속정보에_반영한다() {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, null);

		when(request.leagueId()).thenReturn(20L);

		League league = mock(League.class);

		when(leagueRepository.findById(20L))
			.thenReturn(Optional.of(league));

		mockSavedPlayer(103L);

		service.createPlayer(request);

		ArgumentCaptor<PlayerTeamDetail> captor =
			ArgumentCaptor.forClass(PlayerTeamDetail.class);

		verify(playerTeamDetailRepository)
			.save(captor.capture());

		assertThat(captor.getValue().getLeague())
			.isSameAs(league);

		verifyNoInteractions(playerExternalMappingRepository);
	}

	// ========================================
	// 2. 선수 등록 실패
	// ========================================

	@Test
	void MANUAL_선수는_API_ID를_입력할_수_없다() {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, "manual-100");

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.PLAYER_MANUAL_API_ID_NOT_ALLOWED
		);

		verify(playerRepository, never()).save(any());

		verifyNoInteractions(playerExternalMappingRepository);

		verify(playerTeamDetailRepository, never())
			.save(any());
	}

	@ParameterizedTest
	@MethodSource("externalProvidersWithMissingApiIds")
	void MANUAL이_아닌_Provider는_API_ID가_필수다(
		DataOrigin provider,
		String apiPlayerId
	) {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(provider, apiPlayerId);

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.PLAYER_API_ID_REQUIRED
		);

		verify(playerRepository, never()).save(any());

		verifyNoInteractions(playerExternalMappingRepository);

		verify(playerTeamDetailRepository, never())
			.save(any());
	}

	@ParameterizedTest
	@EnumSource(
		value = DataOrigin.class,
		mode = EnumSource.Mode.EXCLUDE,
		names = "MANUAL"
	)
	void 동일한_Provider와_API_ID는_중복등록할_수_없다(
		DataOrigin provider
	) {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(provider, "12345");

		PlayerExternalMapping existingMapping =
			mock(PlayerExternalMapping.class);

		when(playerExternalMappingRepository
			.findByProviderAndApiPlayerId(provider, "12345"))
			.thenReturn(Optional.of(existingMapping));

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.PLAYER_API_ID_ALREADY_EXISTS
		);

		verify(playerRepository, never()).save(any());

		verify(playerExternalMappingRepository, never())
			.save(any());

		verify(playerTeamDetailRepository, never())
			.save(any());
	}

	@Test
	void Provider가_없으면_등록할_수_없다() {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(null, null);

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.PLAYER_PROVIDER_REQUIRED
		);

		verify(playerRepository, never()).save(any());
	}

	@Test
	void 선수와_팀의_종목이_다르면_등록할_수_없다() {
		Sport sport = mock(Sport.class);
		Sport teamSport = mock(Sport.class);
		Team team = mock(Team.class);

		when(sportRepository.findById(1L))
			.thenReturn(Optional.of(sport));

		when(teamRepository.findById(10L))
			.thenReturn(Optional.of(team));

		when(sport.getId()).thenReturn(1L);
		when(team.getSport()).thenReturn(teamSport);
		when(teamSport.getId()).thenReturn(2L);

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, null);

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.PLAYER_SPORT_MISMATCH
		);

		verify(playerRepository, never()).save(any());
	}

	@Test
	void 존재하지_않는_종목이면_등록할_수_없다() {
		AdminPlayerCreateRequest request =
			mock(AdminPlayerCreateRequest.class);

		when(request.sportId()).thenReturn(999L);

		when(sportRepository.findById(999L))
			.thenReturn(Optional.empty());

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.SPORT_NOT_FOUND
		);

		verify(playerRepository, never()).save(any());
	}

	@Test
	void 존재하지_않는_팀이면_등록할_수_없다() {
		Sport sport = mock(Sport.class);

		when(sportRepository.findById(1L))
			.thenReturn(Optional.of(sport));

		when(teamRepository.findById(999L))
			.thenReturn(Optional.empty());

		AdminPlayerCreateRequest request =
			mock(AdminPlayerCreateRequest.class);

		when(request.sportId()).thenReturn(1L);
		when(request.teamId()).thenReturn(999L);

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.TEAM_NOT_FOUND
		);

		verify(playerRepository, never()).save(any());
	}

	@Test
	void 존재하지_않는_리그이면_등록할_수_없다() {
		mockValidSportAndTeam();

		AdminPlayerCreateRequest request =
			createRequest(DataOrigin.MANUAL, null);

		when(request.leagueId()).thenReturn(999L);

		when(leagueRepository.findById(999L))
			.thenReturn(Optional.empty());

		assertBusinessError(
			() -> service.createPlayer(request),
			ErrorCode.LEAGUE_NOT_FOUND
		);

		verify(playerRepository, never()).save(any());
	}

	// ========================================
	// 3. 스쿼드 상태 변경
	// ========================================

	@Test
	void 스쿼드_상태를_수정한다() {
		PlayerTeamDetail detail =
			mock(PlayerTeamDetail.class);

		AdminPlayerSquadUpdateRequest request =
			mock(AdminPlayerSquadUpdateRequest.class);

		when(playerTeamDetailRepository.findById(1L))
			.thenReturn(Optional.of(detail));

		when(request.squadOn()).thenReturn(true);

		service.updateSquadOn(1L, request);

		verify(detail).updateSquadOn(true);
	}

	@Test
	void 존재하지_않는_팀소속은_스쿼드를_수정할_수_없다() {
		AdminPlayerSquadUpdateRequest request =
			mock(AdminPlayerSquadUpdateRequest.class);

		when(playerTeamDetailRepository.findById(999L))
			.thenReturn(Optional.empty());

		assertBusinessError(
			() -> service.updateSquadOn(999L, request),
			ErrorCode.PLAYER_TEAM_DETAIL_NOT_FOUND
		);
	}

	// ========================================
	// 4. 선수 정보 수정
	// ========================================

	@Test
	void 선수_이름과_태극전사_상태를_수정한다() {
		Player player = mock(Player.class);

		AdminPlayerUpdateRequest request =
			mock(AdminPlayerUpdateRequest.class);

		when(playerRepository.findById(100L))
			.thenReturn(Optional.of(player));

		when(request.kName()).thenReturn("손흥민");
		when(request.taegeuk()).thenReturn(true);

		when(player.getEName())
			.thenReturn("Son Heung-min");

		service.updatePlayer(100L, request);

		verify(player).updateNames(
			"손흥민",
			"Son Heung-min"
		);

		verify(player).updateTaegeuk(true);
	}

	@Test
	void 존재하지_않는_선수는_수정할_수_없다() {
		AdminPlayerUpdateRequest request =
			mock(AdminPlayerUpdateRequest.class);

		when(playerRepository.findById(999L))
			.thenReturn(Optional.empty());

		assertBusinessError(
			() -> service.updatePlayer(999L, request),
			ErrorCode.PLAYER_NOT_FOUND
		);
	}

	// ========================================
	// 5. 선수 팀 소속 정보 삭제
	// ========================================

	@Test
	void 선수의_팀소속정보를_삭제한다() {
		PlayerTeamDetail detail =
			mock(PlayerTeamDetail.class);

		when(playerTeamDetailRepository.findById(1L))
			.thenReturn(Optional.of(detail));

		service.deletePlayerTeamDetail(1L);

		verify(playerTeamDetailRepository).delete(detail);
	}

	@Test
	void 존재하지_않는_팀소속정보는_삭제할_수_없다() {
		when(playerTeamDetailRepository.findById(999L))
			.thenReturn(Optional.empty());

		assertBusinessError(
			() -> service.deletePlayerTeamDetail(999L),
			ErrorCode.PLAYER_TEAM_DETAIL_NOT_FOUND
		);

		verify(playerTeamDetailRepository, never())
			.delete(any());
	}

	// ========================================
	// 6. 선수 검색
	// ========================================

	@Test
	void 검색어를_정규화해서_조회한다() {
		Pageable pageable = PageRequest.of(0, 20);

		when(playerRepository.searchAdminPlayers(
			1L,
			null,
			"Son",
			null,
			"10",
			10L,
			pageable
		)).thenReturn(Page.empty(pageable));

		Page<?> result = service.searchPlayers(
			1L,
			null,
			"  Son  ",
			"  10  ",
			pageable
		);

		assertThat(result).isEmpty();

		verify(playerRepository).searchAdminPlayers(
			1L,
			null,
			"Son",
			null,
			"10",
			10L,
			pageable
		);

		verifyNoInteractions(
			playerTeamDetailRepository,
			playerExternalMappingRepository
		);
	}

	@Test
	void 검색어가_공백이면_null로_조회한다() {
		Pageable pageable = PageRequest.of(0, 20);

		when(playerRepository.searchAdminPlayers(
			1L,
			null,
			null,
			null,
			null,
			null,
			pageable
		)).thenReturn(Page.empty(pageable));

		Page<?> result = service.searchPlayers(
			1L,
			null,
			"   ",
			" ",
			pageable
		);

		assertThat(result).isEmpty();

		verify(playerRepository).searchAdminPlayers(
			1L,
			null,
			null,
			null,
			null,
			null,
			pageable
		);
	}

	@Test
	void 숫자가_아닌_검색어는_ID로_변환하지_않는다() {
		Pageable pageable = PageRequest.of(0, 20);

		when(playerRepository.searchAdminPlayers(
			1L,
			null,
			"Son123",
			null,
			"Team10",
			null,
			pageable
		)).thenReturn(Page.empty(pageable));

		service.searchPlayers(
			1L,
			null,
			"Son123",
			"Team10",
			pageable
		);

		verify(playerRepository).searchAdminPlayers(
			1L,
			null,
			"Son123",
			null,
			"Team10",
			null,
			pageable
		);
	}

	@Test
	void Long_범위를_초과한_검색어는_ID로_변환하지_않는다() {
		Pageable pageable = PageRequest.of(0, 20);

		String keyword = "999999999999999999999999999";

		when(playerRepository.searchAdminPlayers(
			1L,
			null,
			keyword,
			null,
			null,
			null,
			pageable
		)).thenReturn(Page.empty(pageable));

		service.searchPlayers(
			1L,
			null,
			keyword,
			null,
			pageable
		);

		verify(playerRepository).searchAdminPlayers(
			1L,
			null,
			keyword,
			null,
			null,
			null,
			pageable
		);
	}

	// ========================================
	// 7. 공통 헬퍼
	// ========================================

	private AdminPlayerCreateRequest createRequest(
		DataOrigin provider,
		String apiPlayerId
	) {
		AdminPlayerCreateRequest request =
			mock(AdminPlayerCreateRequest.class);

		when(request.sportId()).thenReturn(1L);
		when(request.teamId()).thenReturn(10L);

		// 리그는 선택값이므로 기본 null
		lenient().when(request.leagueId()).thenReturn(null);

		// 검증 단계에서 예외가 발생할 수 있으므로 lenient 사용
		if (provider != null) {
			lenient().when(request.provider())
				.thenReturn(provider);
		}

		if (apiPlayerId != null) {
			lenient().when(request.apiPlayerId())
				.thenReturn(apiPlayerId);
		}

		return request;
	}

	private void mockValidSportAndTeam() {
		Sport sport = mock(Sport.class);
		Team team = mock(Team.class);

		when(sportRepository.findById(1L))
			.thenReturn(Optional.of(sport));

		when(teamRepository.findById(10L))
			.thenReturn(Optional.of(team));

		when(sport.getId()).thenReturn(1L);
		when(team.getSport()).thenReturn(sport);
	}

	private Player mockSavedPlayer(Long playerId) {
		Player savedPlayer = mock(Player.class);

		when(playerRepository.save(any(Player.class)))
			.thenReturn(savedPlayer);

		when(savedPlayer.getId()).thenReturn(playerId);

		return savedPlayer;
	}

	private static void assertBusinessError(
		ThrowingCallable action,
		ErrorCode expectedErrorCode
	) {
		assertThatThrownBy(action)
			.isInstanceOfSatisfying(
				BusinessException.class,
				e -> assertThat(e.getErrorCode())
					.isEqualTo(expectedErrorCode)
			);
	}

	private static Stream<Arguments> externalProvidersWithMissingApiIds() {
		return Arrays.stream(DataOrigin.values())
			.filter(provider -> provider != DataOrigin.MANUAL)
			.flatMap(provider ->
				Stream.of((String)null, "", " ", "   ")
					.map(apiPlayerId ->
						Arguments.of(provider, apiPlayerId)
					)
			);
	}
}
