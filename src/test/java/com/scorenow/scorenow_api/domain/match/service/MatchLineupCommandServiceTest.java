package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerAddRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayerRole;
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

	@InjectMocks
	private MatchLineupCommandService service;

	private MatchLineupDocument document;
	private LineupSide home;
	private List<LineupPlayer> starters;
	private List<LineupPlayer> substitutes;

	@BeforeEach
	void setUp() {
		starters = new ArrayList<>();
		substitutes = new ArrayList<>();

		home = LineupSide.builder()
			.teamId(10L)
			.apiTeamId("api-team-10")
			.teamEname("Home Team")
			.formation("4-4-2")
			.uniformColor("#000000")
			.startingLineup(starters)
			.substitutes(substitutes)
			.build();

		document = MatchLineupDocument.create(
			1L,
			1L,
			"api-match-1"
		);
		document.setHome(home);

		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
	}

	@Test
	void 선발_선수를_라인업에_추가한다() {
		MatchLineupPlayerAddRequest request =
			new MatchLineupPlayerAddRequest(
				100L,
				LineupPlayerRole.STARTER
			);

		MatchLineupPlayerResponse playerInfo = createPlayerInfo();

		when(matchLineupSearchRepository.findPlayerByIdAndTeamId(
			100L,
			10L
		)).thenReturn(Optional.of(playerInfo));

		String result = service.addPlayerToLineup(
			1L,
			10L,
			request
		);

		assertThat(result).isEqualTo("1:100");
		assertThat(substitutes).isEmpty();

		assertThat(starters)
			.singleElement()
			.satisfies(player -> {
				assertThat(player.getPlayerId()).isEqualTo(100L);
				assertThat(player.getKName()).isEqualTo("선수");
				assertThat(player.getEName()).isEqualTo("Player");
				assertThat(player.getPosition()).isEqualTo("MF");
				assertThat(player.getShirtNumber()).isEqualTo("7");
				assertThat(player.getGoals()).isZero();
				assertThat(player.isTemp()).isFalse();
			});

		verify(matchLineupSearchRepository)
			.findPlayerByIdAndTeamId(100L, 10L);
		verify(matchLineupRepo).save(document);
	}

	@Test
	void 교체_선수를_교체_명단에_추가한다() {
		MatchLineupPlayerAddRequest request =
			new MatchLineupPlayerAddRequest(
				100L,
				LineupPlayerRole.SUBSTITUTE
			);

		MatchLineupPlayerResponse playerInfo = createPlayerInfo();

		when(matchLineupSearchRepository.findPlayerByIdAndTeamId(
			100L,
			10L
		)).thenReturn(Optional.of(playerInfo));

		String result = service.addPlayerToLineup(
			1L,
			10L,
			request
		);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).isEmpty();

		assertThat(substitutes)
			.singleElement()
			.satisfies(player -> {
				assertThat(player.getPlayerId()).isEqualTo(100L);
				assertThat(player.getKName()).isEqualTo("선수");
				assertThat(player.getEName()).isEqualTo("Player");
				assertThat(player.getPosition()).isEqualTo("MF");
				assertThat(player.getShirtNumber()).isEqualTo("7");
				assertThat(player.getGoals()).isZero();
				assertThat(player.isTemp()).isFalse();
			});

		verify(matchLineupSearchRepository)
			.findPlayerByIdAndTeamId(100L, 10L);
		verify(matchLineupRepo).save(document);
	}

	@Test
	void 이미_라인업에_존재하는_선수는_추가할_수_없다() {
		LineupPlayer existingPlayer = LineupPlayer.builder()
			.playerId(100L)
			.eName("Existing Player")
			.build();

		starters.add(existingPlayer);

		MatchLineupPlayerAddRequest request =
			new MatchLineupPlayerAddRequest(
				100L,
				LineupPlayerRole.STARTER
			);

		assertThatThrownBy(() ->
			service.addPlayerToLineup(
				1L,
				10L,
				request
			)
		).isInstanceOf(BusinessException.class);

		verify(
			matchLineupSearchRepository,
			never()
		).findPlayerByIdAndTeamId(anyLong(), anyLong());

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 해당_팀의_선수_정보가_없으면_라인업에_추가할_수_없다() {
		MatchLineupPlayerAddRequest request =
			new MatchLineupPlayerAddRequest(
				100L,
				LineupPlayerRole.STARTER
			);

		when(matchLineupSearchRepository.findPlayerByIdAndTeamId(
			100L,
			10L
		)).thenReturn(Optional.empty());

		assertThatThrownBy(() ->
			service.addPlayerToLineup(
				1L,
				10L,
				request
			)
		).isInstanceOf(BusinessException.class);

		assertThat(starters).isEmpty();
		assertThat(substitutes).isEmpty();

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 홈팀의_선발_선수를_라인업에서_제거한다() {
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.eName("Player")
			.build();

		starters.add(player);

		String result = service.removePlayerFromLineup(
			1L,
			10L,
			100L
		);

		assertThat(result).isEqualTo("1:100");
		assertThat(starters).isEmpty();

		verify(matchLineupRepo).save(document);
	}

	@Test
	void 해당_팀의_라인업에_없는_선수는_제거할_수_없다() {
		assertThatThrownBy(() ->
			service.removePlayerFromLineup(
				1L,
				10L,
				100L
			)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 팀의_포메이션과_유니폼_컬러를_수정한다() {
		MatchLineupSideUpdateRequest request =
			mock(MatchLineupSideUpdateRequest.class);

		when(request.getFormation()).thenReturn("4-3-3");
		when(request.getUniformColor()).thenReturn("#FFFFFF");

		String result = service.updateLineupSide(
			1L,
			10L,
			request
		);

		assertThat(result).isEqualTo("1:10");
		assertThat(home.getFormation()).isEqualTo("4-3-3");
		assertThat(home.getUniformColor()).isEqualTo("#FFFFFF");

		verify(matchLineupRepo).save(document);
	}

	@Test
	void 홈팀_선수의_포지션_등번호_득점_정보를_수정한다() {
		LineupPlayer player = LineupPlayer.builder()
			.playerId(100L)
			.eName("Player")
			.position("MF")
			.shirtNumber("7")
			.goals(0)
			.temp(false)
			.build();

		starters.add(player);

		MatchLineupPlayerUpdateRequest request =
			mock(MatchLineupPlayerUpdateRequest.class);

		when(request.getPosition()).thenReturn("FW");
		when(request.getShirtNumber()).thenReturn("9");
		when(request.getGoals()).thenReturn(1);

		String result = service.updateLineupManual(
			1L,
			10L,
			100L,
			request
		);

		assertThat(result).isEqualTo("1:100");
		assertThat(player.getPosition()).isEqualTo("FW");
		assertThat(player.getShirtNumber()).isEqualTo("9");
		assertThat(player.getGoals()).isEqualTo(1);

		verify(matchLineupRepo).save(document);
	}

	private MatchLineupPlayerResponse createPlayerInfo() {
		MatchLineupPlayerResponse playerInfo =
			mock(MatchLineupPlayerResponse.class);

		when(playerInfo.getPlayerId()).thenReturn(100L);
		when(playerInfo.getKName()).thenReturn("선수");
		when(playerInfo.getEName()).thenReturn("Player");
		when(playerInfo.getPosition()).thenReturn("MF");
		when(playerInfo.getShirtNumber()).thenReturn("7");

		return playerInfo;
	}
}