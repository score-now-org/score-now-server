package com.scorenow.scorenow_api.domain.match.util;

public class MatchIdParser {

	private MatchIdParser() {
	}

	/** matchId: sportId + eventId */
	public static String extractEventId(String matchId, String sportId) {
		return matchId.substring(sportId.length());
	}
}
