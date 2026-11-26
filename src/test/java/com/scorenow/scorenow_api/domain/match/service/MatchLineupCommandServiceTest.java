package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerAddRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerRoleUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchLineupSearchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MatchLineupCommandServiceTest {

	@Mock
	private MatchLineupRepository matchLineupRepo;

	@Mock
	private MatchLineupSearchRepository matchLineupSearchRepository;

	@Mock
	private MatchLineupMapper lineupMapper;

	@Mock
	private MatchLineupFormationCalculator formationCalculator;

	@InjectMocks
	private MatchLineupCommandService service;

	private MatchLineupDocument document;
	private LineupSide home;
	private List<LineupPlayer> starters;
	private List<LineupPlayer> substitutes;

	private final DataOrigin provider = DataOrigin.values()[0];

	@BeforeEach
	void setUp() {
		document = mock(MatchLineupDocument.class);
		home = mock(LineupSide.class);

		starters = new ArrayList<>();
		substitutes = new ArrayList<>();

		lenient().when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));

		lenient().when(document.getHome())
			.thenReturn(home);

		lenient().when(home.getTeamId())
			.thenReturn(10L);

		lenient().when(home.getStartingLineup())
			.thenReturn(starters);

		lenient().when(home.getSubstitutes())
			.thenReturn(substitutes);
	}

	@Test
	void addPlayerToLineup_starter() throws Exception {
		MatchLineupPlayerAddRequest request =
			addRequest(100L, "STARTER");

		when(matchLineupSearchRepository.findPlayerByIdAndTeamId(100L, 10L))
			.thenReturn(Optional.of(playerResponse(100L, "Forward")));

		when(lineupMapper.mapPosition("Forward"))
			.thenReturn("FW");

		when(formationCalculator.calculate(starters)).thenReturn("0-0-1");

		String result =
			service.addPlayerToLineup(1L, 10L, request);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).hasSize(1);
		assertThat(substitutes).isEmpty();

		LineupPlayer added = starters.get(0);

		assertThat(added.getPlayerId()).isEqualTo(100L);
		assertThat(added.getPosition()).isEqualTo("FW");
		assertThat(added.getShirtNumber()).isEqualTo("7");

		verify(lineupMapper).mapPosition("Forward");
		verify(formationCalculator).calculate(starters);
		verify(home).setFormation("0-0-1");
		verify(matchLineupRepo).save(document);
	}

	@Test
	void addPlayerToLineup_substitute() throws Exception {
		MatchLineupPlayerAddRequest request =
			addRequest(100L, "SUBSTITUTE");

		when(matchLineupSearchRepository.findPlayerByIdAndTeamId(100L, 10L))
			.thenReturn(Optional.of(playerResponse(100L, "Guard")));

		when(lineupMapper.mapPosition("Guard"))
			.thenReturn("GK");

		String result =
			service.addPlayerToLineup(1L, 10L, request);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).isEmpty();
		assertThat(substitutes).hasSize(1);
		assertThat(substitutes.get(0).getPosition()).isEqualTo("GK");

		verify(matchLineupRepo).save(document);
	}

	@Test
	void addPlayerToLineup_duplicate() throws Exception {
		starters.add(LineupPlayer.builder()
			.playerId(100L)
			.build());

		MatchLineupPlayerAddRequest request =
			addRequest(100L, "STARTER");

		assertThatThrownBy(
			() -> service.addPlayerToLineup(1L, 10L, request)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupSearchRepository, never())
			.findPlayerByIdAndTeamId(anyLong(), anyLong());

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void removePlayerFromLineup() {
		LineupPlayer defender = LineupPlayer.builder()
			.playerId(200L)
			.position("DF")
			.build();

		LineupPlayer forward = LineupPlayer.builder()
			.playerId(100L)
			.position("FW")
			.build();

		starters.add(defender);
		starters.add(forward);

		when(formationCalculator.calculate(starters))
			.thenReturn("1-0-0");

		String result =
			service.removePlayerFromLineup(1L, 10L, 100L);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).containsExactly(defender);

		verify(formationCalculator).calculate(starters);
		verify(home).setFormation("1-0-0");
		verify(matchLineupRepo).save(document);
	}

	@Test
	void removePlayerFromLineup_notFound() {
		assertThatThrownBy(
			() -> service.removePlayerFromLineup(1L, 10L, 999L)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void updateLineupSide() {
		MatchLineupSideUpdateRequest request =
			mock(MatchLineupSideUpdateRequest.class);

		when(request.getUniformColor()).thenReturn("#FFFFFF");
		when(home.getUniformColor()).thenReturn("#000000");

		String result =
			service.updateLineupSide(1L, 10L, request);

		assertThat(result).isEqualTo("1:10");

		verify(home).setUniformColor("#FFFFFF");
		verify(home, never()).setFormation(any());
		verify(matchLineupRepo).save(document);
	}

	@Test
	void updateLineupManual() {
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.position("MF")
			.shirtNumber("7")
			.build();

		starters.add(player);

		MatchLineupPlayerUpdateRequest request =
			mock(MatchLineupPlayerUpdateRequest.class);

		when(request.getPosition()).thenReturn("FW");
		when(request.getShirtNumber()).thenReturn("9");

		// 포메이션 계산 결과 설정
		when(formationCalculator.calculate(starters))
			.thenReturn("0-0-1");

		String result =
			service.updateLineupManual(1L, 10L, 100L, request);

		assertThat(result).isEqualTo("1:100");

		assertThat(player.getPosition()).isEqualTo("FW");
		assertThat(player.getShirtNumber()).isEqualTo("9");

		// 포메이션 갱신 검증
		verify(formationCalculator).calculate(starters);
		verify(home).setFormation("0-0-1");

		verify(matchLineupRepo).save(document);
	}

	@Test
	void updateLineupManual_noChanges() {
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.position("MF")
			.shirtNumber("7")
			.build();

		starters.add(player);

		MatchLineupPlayerUpdateRequest request =
			mock(MatchLineupPlayerUpdateRequest.class);

		when(request.getPosition()).thenReturn("MF");

		String result =
			service.updateLineupManual(1L, 10L, 100L, request);

		assertThat(result).isEqualTo("NO_CHANGES");

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void addPlayerToLineup_invalidMatchId() {
		assertThatThrownBy(
			() -> service.addPlayerToLineup(null, 10L, null)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(matchLineupRepo);
	}

	@Test
	void updateLineupPlayerRole_toStarter() {
		// given
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.position("FW")
			.shirtNumber("7")
			.build();

		substitutes.add(player);

		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(MatchLineupPlayerRoleUpdateRequest.Role.STARTER);

		when(formationCalculator.calculate(starters))
			.thenReturn("0-0-1");

		// when
		String result =
			service.updateLineupPlayerRole(1L, 10L, 100L, request);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).containsExactly(player);
		assertThat(substitutes).isEmpty();

		assertThat(player.getPosition()).isEqualTo("FW");
		assertThat(player.getShirtNumber()).isEqualTo("7");

		verify(formationCalculator).calculate(starters);
		verify(home).setFormation("0-0-1");
		verify(matchLineupRepo).save(document);
	}

	@Test
	void updateLineupPlayerRole_toSubstitute() {
		LineupPlayer defender = LineupPlayer.builder()
			.playerId(100L)
			.position("DF")
			.build();

		LineupPlayer midfielder = LineupPlayer.builder()
			.playerId(200L)
			.position("MF")
			.build();

		starters.add(defender);
		starters.add(midfielder);

		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(
				MatchLineupPlayerRoleUpdateRequest.Role.SUBSTITUTE
			);

		when(formationCalculator.calculate(starters))
			.thenReturn("0-1-0");

		String result =
			service.updateLineupPlayerRole(1L, 10L, 100L, request);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).containsExactly(midfielder);
		assertThat(substitutes).containsExactly(defender);

		verify(formationCalculator).calculate(starters);
		verify(home).setFormation("0-1-0");
		verify(matchLineupRepo).save(document);
	}

	@Test
	void updateLineupPlayerRole_noChanges() {
		starters.add(LineupPlayer.builder()
			.playerId(100L)
			.build());

		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(MatchLineupPlayerRoleUpdateRequest.Role.STARTER);

		String result =
			service.updateLineupPlayerRole(1L, 10L, 100L, request);

		assertThat(result).isEqualTo("NO_CHANGES");
		assertThat(starters).hasSize(1);

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void updateLineupPlayerRole_notFound() {
		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(MatchLineupPlayerRoleUpdateRequest.Role.STARTER);

		assertThatThrownBy(
			() -> service.updateLineupPlayerRole(1L, 10L, 999L, request)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void updateLineupPlayerRole_duplicate() {
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.build();

		starters.add(player);
		substitutes.add(player);

		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(MatchLineupPlayerRoleUpdateRequest.Role.STARTER);

		assertThatThrownBy(
			() -> service.updateLineupPlayerRole(1L, 10L, 100L, request)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void updateLineupPlayerRole_invalidRole() {
		MatchLineupPlayerRoleUpdateRequest request =
			new MatchLineupPlayerRoleUpdateRequest(null);

		assertThatThrownBy(
			() -> service.updateLineupPlayerRole(1L, 10L, 100L, request)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(matchLineupRepo);
	}

	private MatchLineupPlayerResponse playerResponse(
		Long playerId,
		String position
	) {
		return new MatchLineupPlayerResponse(
			playerId,
			provider,
			"api-" + playerId,
			"선수" + playerId,
			"Player " + playerId,
			position,
			10L,
			"Test Team",
			"7",
			false
		);
	}

	// role enum의 실제 클래스명이 제공되지 않아
	// STARTER/SUBSTITUTE를 이름으로 가져오는 테스트 헬퍼
	private MatchLineupPlayerAddRequest addRequest(
		Long playerId,
		String roleName
	) throws Exception {
		Class<?> roleType =
			MatchLineupPlayerAddRequest.class
				.getMethod("role")
				.getReturnType();

		Object role = Arrays.stream(roleType.getEnumConstants())
			.filter(value -> ((Enum<?>)value).name().equals(roleName))
			.findFirst()
			.orElseThrow();

		MatchLineupPlayerAddRequest request =
			mock(MatchLineupPlayerAddRequest.class);

		when(request.playerId()).thenReturn(playerId);
		doReturn(role).when(request).role();

		return request;
	}
}
