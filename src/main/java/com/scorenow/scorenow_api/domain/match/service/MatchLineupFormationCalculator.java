package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;

@Component
public class MatchLineupFormationCalculator {

	public String calculate(List<LineupPlayer> startingLineup) {
		if (startingLineup == null || startingLineup.isEmpty()) {
			return null;
		}

		int df = 0;
		int mf = 0;
		int fw = 0;

		for (LineupPlayer player : startingLineup) {
			if (player == null || player.getPosition() == null) {
				continue;
			}

			switch (player.getPosition().toUpperCase(Locale.ROOT)) {
				case "DF" -> df++;
				case "MF" -> mf++;
				case "FW" -> fw++;
				default -> {
					// GK 및 기타 포지션 제외
				}
			}
		}

		if (df + mf + fw == 0) {
			return null;
		}

		return df + "-" + mf + "-" + fw;
	}
}
