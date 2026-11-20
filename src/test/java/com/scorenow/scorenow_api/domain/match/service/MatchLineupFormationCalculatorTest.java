package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;

class MatchLineupFormationCalculatorTest {

	private final MatchLineupFormationCalculator calculator =
		new MatchLineupFormationCalculator();

	@Test
	void calculate_442() {
		List<LineupPlayer> starters = List.of(
			player("GK"),
			player("DF"), player("DF"),
			player("DF"), player("DF"),
			player("MF"), player("MF"),
			player("MF"), player("MF"),
			player("FW"), player("FW")
		);

		assertThat(calculator.calculate(starters))
			.isEqualTo("4-4-2");
	}

	@Test
	void calculate_352() {
		List<LineupPlayer> starters = List.of(
			player("GK"),
			player("DF"), player("DF"), player("DF"),
			player("MF"), player("MF"), player("MF"),
			player("MF"), player("MF"),
			player("FW"), player("FW")
		);

		assertThat(calculator.calculate(starters))
			.isEqualTo("3-5-2");
	}

	@Test
	void calculate_emptyLineup() {
		assertThat(calculator.calculate(List.of()))
			.isNull();
	}

	@Test
	void calculate_nullLineup() {
		assertThat(calculator.calculate(null))
			.isNull();
	}

	@Test
	void calculate_onlyGoalkeeper() {
		assertThat(calculator.calculate(List.of(player("GK"))))
			.isNull();
	}

	@Test
	void calculate_partialLineup() {
		List<LineupPlayer> starters = List.of(
			player("GK"),
			player("DF"),
			player("MF"),
			player("FW")
		);

		assertThat(calculator.calculate(starters))
			.isEqualTo("1-1-1");
	}

	private LineupPlayer player(String position) {
		return LineupPlayer.builder()
			.position(position)
			.build();
	}
}
