package com.scorenow.scorenow_api.domain.match.util;

public class MatchIdParser {

	private MatchIdParser() {
	}

	// matchId: BETS + sportNo(1자리) + eventId
	// sportId: BETS + sportNo
	public static String extractSportId(String matchId) {
		if (matchId == null || !matchId.startsWith("BETS") || matchId.length() < 6) {
			throw new IllegalArgumentException("invalid matchId: " + matchId);
		}
		return matchId.substring(0, 5); // 예: BETS1
	}

	public static String extractEventId(String matchId) {
		String sportId = extractSportId(matchId);
		return matchId.substring(sportId.length()); // sportId(BETS1) 떼고 나머지
	}
}
