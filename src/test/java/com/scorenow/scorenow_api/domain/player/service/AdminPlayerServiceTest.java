package com.scorenow.scorenow_api.domain.player.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerResponse;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.team.entity.Team;

@ExtendWith(MockitoExtension.class)
class AdminPlayerServiceTest {

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private PlayerTeamDetailRepository playerTeamDetailRepository;

	@InjectMocks
	private AdminPlayerService adminPlayerService;

	private Pageable pageable;

	@BeforeEach
	void setUp() {
		pageable = PageRequest.of(0, 20);
	}

	@Test
	@DisplayName("검색 조건이 없으면 전체 선수 목록을 조회한다")
	void searchAllPlayers() {
		// given
		Player player = createPlayer();
		mockPlayerSearch(List.of(player));

		// when
		Page<AdminPlayerResponse> result =
			adminPlayerService.searchPlayers(
				null, null, null, null, pageable
			);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(100L);
		assertThat(result.getContent().get(0).eName())
			.isEqualTo("Son Heung-min");

		verify(playerRepository).searchAdminPlayers(
			null, null, null, null, null, null, pageable
		);
	}

	@Test
	@DisplayName("선수명과 팀명의 앞뒤 공백을 제거한다")
	void normalizeKeywords() {
		// given
		mockPlayerSearch(List.of());

		// when
		adminPlayerService.searchPlayers(
			null, null, " Son ", " Suwon ", pageable
		);

		// then
		verify(playerRepository).searchAdminPlayers(
			null, null, "Son", null, "Suwon", null, pageable
		);
	}

	@Test
	@DisplayName("숫자 검색어는 선수 ID와 팀 ID로 변환한다")
	void searchById() {
		// given
		mockPlayerSearch(List.of());

		// when
		adminPlayerService.searchPlayers(
			null, null, "100", "3", pageable
		);

		// then
		verify(playerRepository).searchAdminPlayers(
			null, null, "100", 100L, "3", 3L, pageable
		);
	}

	@Test
	@DisplayName("종목과 리그를 포함한 복합 검색 조건을 전달한다")
	void searchWithMultipleConditions() {
		// given
		mockPlayerSearch(List.of());

		// when
		adminPlayerService.searchPlayers(
			1L, 2L, "Son", "Suwon", pageable
		);

		// then
		verify(playerRepository).searchAdminPlayers(
			1L, 2L, "Son", null, "Suwon", null, pageable
		);
	}

	@Test
	@DisplayName("선수의 팀 및 리그 정보를 함께 반환한다")
	void searchWithTeamDetails() {
		// given
		Player player = createPlayer();
		mockPlayerSearch(List.of(player));

		Team team = mock(Team.class);
		when(team.getId()).thenReturn(3L);
		when(team.resolveTeamName()).thenReturn("Suwon Bluewings");

		League league = mock(League.class);
		when(league.getId()).thenReturn(2L);
		when(league.resolveLeagueName())
			.thenReturn("South Korea K League 2");

		PlayerTeamDetail detail = mock(PlayerTeamDetail.class);
		when(detail.getPlayer()).thenReturn(player);
		when(detail.getTeam()).thenReturn(team);
		when(detail.getLeague()).thenReturn(league);
		when(detail.getPosition()).thenReturn("Forward");
		when(detail.getShirtNumber()).thenReturn("7");

		when(playerTeamDetailRepository.findAllByPlayerIds(
			List.of(100L)
		)).thenReturn(List.of(detail));

		// when
		Page<AdminPlayerResponse> result =
			adminPlayerService.searchPlayers(
				1L, 2L, null, "Suwon", pageable
			);

		// then
		AdminPlayerResponse response = result.getContent().get(0);

		assertThat(response.teams()).hasSize(1);

		var teamResponse = response.teams().get(0);

		assertThat(teamResponse.teamId()).isEqualTo(3L);
		assertThat(teamResponse.teamName())
			.isEqualTo("Suwon Bluewings");
		assertThat(teamResponse.leagueId()).isEqualTo(2L);
		assertThat(teamResponse.leagueName())
			.isEqualTo("South Korea K League 2");
		assertThat(teamResponse.position()).isEqualTo("Forward");
		assertThat(teamResponse.shirtNumber()).isEqualTo("7");

		verify(playerTeamDetailRepository)
			.findAllByPlayerIds(List.of(100L));
	}

	@Test
	@DisplayName("소속팀이 없는 선수는 빈 팀 목록을 반환한다")
	void playerWithoutTeam() {
		// given
		Player player = createPlayer();
		mockPlayerSearch(List.of(player));

		// when
		Page<AdminPlayerResponse> result =
			adminPlayerService.searchPlayers(
				null, null, "Son", null, pageable
			);

		// then
		assertThat(result.getContent().get(0).teams()).isEmpty();
	}

	@Test
	@DisplayName("조회 결과가 없으면 소속팀 Repository를 호출하지 않는다")
	void emptySearchResult() {
		// given
		mockPlayerSearch(List.of());

		// when
		Page<AdminPlayerResponse> result =
			adminPlayerService.searchPlayers(
				null, null, "Unknown", null, pageable
			);

		// then
		assertThat(result.isEmpty()).isTrue();
		verifyNoInteractions(playerTeamDetailRepository);
	}

	private Player createPlayer() {
		Sport sport = mock(Sport.class);
		when(sport.getId()).thenReturn(1L);
		when(sport.resolveSportName()).thenReturn("축구");

		return Player.builder()
			.id(100L)
			.sport(sport)
			.eName("Son Heung-min")
			.kName("손흥민")
			.build();
	}

	private void mockPlayerSearch(List<Player> players) {
		Page<Player> page =
			new PageImpl<>(players, pageable, players.size());

		when(playerRepository.searchAdminPlayers(
			nullable(Long.class),
			nullable(Long.class),
			nullable(String.class),
			nullable(Long.class),
			nullable(String.class),
			nullable(Long.class),
			eq(pageable)
		)).thenReturn(page);
	}
}
