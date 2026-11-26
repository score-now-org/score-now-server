package com.scorenow.scorenow_api.domain.match.mapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupFormationCalculator;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

class MatchLineupMapperTest {

	private final MatchLineupMapper mapper =
		new MatchLineupMapper(new MatchLineupFormationCalculator());

	@Test
	void toSide_calculatesFormationFromStarters() {
		// given
		BetsLineupResponse.LineupSide betsSide =
			mock(BetsLineupResponse.LineupSide.class);

		List<BetsLineupResponse.LineupPlayer> starters = List.of(
			player("Guard"),
			player("Defender"),
			player("Defender"),
			player("Midfielder"),
			player("Forward")
		);

		when(betsSide.getStartinglineup()).thenReturn(starters);

		// when
		LineupSide result = mapper.toSide(
			betsSide, 10L, "api-10", "Test Team", Map.of()
		);

		// then
		assertThat(result.getFormation()).isEqualTo("2-1-1");
		assertThat(result.getStartingLineup()).hasSize(5);

		verify(betsSide, never()).getFormation();
	}

	@Test
	void toSide_withoutApiLineup() {
		LineupSide result = mapper.toSide(
			null, 10L, "api-10", "Test Team", Map.of()
		);

		assertThat(result.getFormation()).isNull();
		assertThat(result.getStartingLineup()).isEmpty();
		assertThat(result.getSubstitutes()).isEmpty();
	}

	private BetsLineupResponse.LineupPlayer player(String position) {
		BetsLineupResponse.LineupPlayer player =
			mock(BetsLineupResponse.LineupPlayer.class);

		when(player.getPos()).thenReturn(position);

		return player;
	}
}
