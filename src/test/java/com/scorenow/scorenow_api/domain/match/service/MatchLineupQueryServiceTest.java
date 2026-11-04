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

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchLineupSearchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MatchLineupQueryServiceTest {

	@Mock
	private MatchLineupSearchRepository matchLineupSearchRepository;

	@Mock
	private MatchLineupRepository matchLineupRepo;

	@Mock
	private MatchLineupMapper lineupMapper;

	@InjectMocks
	private MatchLineupQueryService service;

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
	void getByMatchId() {
		MatchLineupDocument result = service.getByMatchId(1L);

		assertThat(result).isSameAs(document);
	}

	@Test
	void getByMatchId_invalidId() {
		assertThatThrownBy(() -> service.getByMatchId(null))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void getSelectablePlayers() {
		// 선발 선수
		starters.add(LineupPlayer.builder()
			.playerId(100L)
			.build());

		// 교체 선수
		substitutes.add(LineupPlayer.builder()
			.playerId(101L)
			.build());

		when(matchLineupSearchRepository.findPlayersByTeamId(10L))
			.thenReturn(List.of(
				playerResponse(100L, "Guard"),
				playerResponse(101L, "Defender"),
				playerResponse(102L, "Forward")
			));

		when(lineupMapper.mapPosition("Guard"))
			.thenReturn("GK");
		when(lineupMapper.mapPosition("Defender"))
			.thenReturn("DF");
		when(lineupMapper.mapPosition("Forward"))
			.thenReturn("FW");

		List<MatchLineupPlayerResponse> result =
			service.getSelectablePlayers(1L, 10L);

		assertThat(result).hasSize(3);

		// 선발, 교체 선수는 선택된 상태
		assertThat(result)
			.extracting("selected")
			.containsExactly(true, true, false);

		// 포지션 변환 확인
		assertThat(result)
			.extracting(MatchLineupPlayerResponse::getPosition)
			.containsExactly("GK", "DF", "FW");

		// 외부 API 매핑 정보 유지
		assertThat(result.get(0).getProvider())
			.isEqualTo(provider);

		assertThat(result.get(0).getApiPlayerId())
			.isEqualTo("api-100");
	}

	@Test
	void searchSelectablePlayers() {
		starters.add(LineupPlayer.builder()
			.playerId(100L)
			.build());

		when(matchLineupSearchRepository.searchSelectablePlayers("Son"))
			.thenReturn(List.of(
				playerResponse(100L, "Forward"),
				playerResponse(200L, "Midfielder")
			));

		when(lineupMapper.mapPosition("Forward"))
			.thenReturn("FW");
		when(lineupMapper.mapPosition("Midfielder"))
			.thenReturn("MF");

		List<MatchLineupPlayerResponse> result =
			service.searchSelectablePlayers(1L, 10L, "  Son  ");

		assertThat(result).hasSize(2);

		assertThat(result)
			.extracting("selected")
			.containsExactly(true, false);

		assertThat(result)
			.extracting(MatchLineupPlayerResponse::getPosition)
			.containsExactly("FW", "MF");

		verify(matchLineupSearchRepository)
			.searchSelectablePlayers("Son");
	}

	@Test
	void getSelectablePlayers_invalidTeamId() {
		assertThatThrownBy(
			() -> service.getSelectablePlayers(1L, null)
		).isInstanceOf(BusinessException.class);
	}

	@Test
	void getSelectablePlayers_teamNotFound() {
		assertThatThrownBy(
			() -> service.getSelectablePlayers(1L, 999L)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupSearchRepository, never())
			.findPlayersByTeamId(anyLong());
	}

	@Test
	void searchSelectablePlayers_blankKeyword() {
		assertThatThrownBy(
			() -> service.searchSelectablePlayers(1L, 10L, "  ")
		).isInstanceOf(BusinessException.class);
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
}
