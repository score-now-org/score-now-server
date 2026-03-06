package com.scorenow.scorenow_api.domain.match.util;

public class IdParser {

	private IdParser() {
	}

	/**
	 * 내부 id에서 apiId 추출
	 * 예: BETS194 -> 94
	 */
	public static String extractApiId(String internalId, String sportId) {
		return internalId.substring(sportId.length());
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
