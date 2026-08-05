package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
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

	@InjectMocks
	private MatchLineupQueryService service;

	private MatchLineupDocument document;
	private LineupSide home;
	private LineupSide away;

	@BeforeEach
	void setUp() {
		document = mock(MatchLineupDocument.class);
		home = mock(LineupSide.class);
		away = mock(LineupSide.class);
	}

	@Test
	void 경기_ID로_라인업을_조회한다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));

		MatchLineupDocument result = service.getByMatchId(1L);

		assertThat(result).isSameAs(document);

		verify(matchLineupRepo).findById(1L);
	}

	@Test
	void 경기_ID가_null이면_라인업을_조회할_수_없다() {
		assertThatThrownBy(() ->
			service.getByMatchId(null)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(matchLineupRepo);
	}

	@Test
	void 라인업이_존재하지_않으면_예외가_발생한다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() ->
			service.getByMatchId(1L)
		).isInstanceOf(BusinessException.class);

		verify(matchLineupRepo).findById(1L);
	}

	@Test
	void 팀_선수_목록에_라인업_반영_여부를_표시한다() {
		LineupPlayer starter = mock(LineupPlayer.class);
		LineupPlayer substitute = mock(LineupPlayer.class);

		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
		when(document.getHome()).thenReturn(home);
		when(home.getTeamId()).thenReturn(10L);

		when(home.getStartingLineup())
			.thenReturn(List.of(starter));
		when(home.getSubstitutes())
			.thenReturn(List.of(substitute));

		when(starter.getPlayerId()).thenReturn(100L);
		when(substitute.getPlayerId()).thenReturn(101L);

		when(matchLineupSearchRepository.findPlayersByTeamId(10L, 30))
			.thenReturn(List.of(
				선수_응답(100L, 10L),
				선수_응답(101L, 10L),
				선수_응답(102L, 10L)
			));

		List<MatchLineupPlayerResponse> result =
			service.getSelectablePlayers(1L, 10L);

		assertThat(result)
			.extracting("playerId", "selected")
			.containsExactly(
				tuple(100L, true),
				tuple(101L, true),
				tuple(102L, false)
			);

		verify(matchLineupSearchRepository)
			.findPlayersByTeamId(10L, 30);
	}

	@Test
	void 경기에_포함되지_않은_팀의_선수_목록은_조회할_수_없다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));

		when(document.getHome()).thenReturn(home);
		when(home.getTeamId()).thenReturn(10L);

		when(document.getAway()).thenReturn(away);
		when(away.getTeamId()).thenReturn(20L);

		assertThatThrownBy(() ->
			service.getSelectablePlayers(1L, 30L)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(matchLineupSearchRepository);
	}

	@Test
	void 검색_결과에_해당_팀의_라인업_반영_여부를_표시한다() {
		LineupPlayer awayStarter = mock(LineupPlayer.class);

		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));

		when(document.getHome()).thenReturn(home);
		when(home.getTeamId()).thenReturn(10L);

		when(document.getAway()).thenReturn(away);
		when(away.getTeamId()).thenReturn(20L);
		when(away.getStartingLineup())
			.thenReturn(List.of(awayStarter));
		when(away.getSubstitutes())
			.thenReturn(List.of());

		when(awayStarter.getPlayerId()).thenReturn(200L);

		when(matchLineupSearchRepository.searchSelectablePlayers("Player", 30))
			.thenReturn(List.of(
				선수_응답(100L, 10L),
				선수_응답(200L, 20L),
				선수_응답(300L, 30L)
			));

		List<MatchLineupPlayerResponse> result =
			service.searchSelectablePlayers(
				1L,
				20L,
				"  Player  "
			);

		assertThat(result)
			.extracting("playerId", "selected")
			.containsExactly(
				tuple(100L, false),
				tuple(200L, true),
				tuple(300L, false)
			);

		verify(matchLineupSearchRepository)
			.searchSelectablePlayers("Player", 30);
	}

	@Test
	void 검색어가_비어있으면_선수를_검색할_수_없다() {
		assertThatThrownBy(() ->
			service.searchSelectablePlayers(
				1L,
				10L,
				"   "
			)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(
			matchLineupRepo,
			matchLineupSearchRepository
		);
	}

	@Test
	void 팀_ID가_null이면_선수를_검색할_수_없다() {
		assertThatThrownBy(() ->
			service.searchSelectablePlayers(
				1L,
				null,
				"Player"
			)
		).isInstanceOf(BusinessException.class);

		verifyNoInteractions(
			matchLineupRepo,
			matchLineupSearchRepository
		);
	}

	private MatchLineupPlayerResponse 선수_응답(
		Long playerId,
		Long teamId
	) {
		return new MatchLineupPlayerResponse(
			playerId,
			"선수" + playerId,
			"Player" + playerId,
			"FW",
			teamId,
			"Team" + teamId,
			"7",
			false
		);
	}
}