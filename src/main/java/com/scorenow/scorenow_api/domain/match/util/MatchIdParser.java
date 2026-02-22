package com.scorenow.scorenow_api.domain.match.util;

public class MatchIdParser {

	private MatchIdParser() {
	}

	/** matchId: sportId + eventId */
	public static String extractEventId(String matchId, String sportId) {

		if (matchId == null || sportId == null || !matchId.startsWith(sportId)) {
			throw new IllegalArgumentException(
				"잘못된 요청: matchId/sportId가 일치하지 않습니다. matchId=" + matchId + ", sportId=" + sportId);
		}
		return matchId.substring(sportId.length());
	}
}
