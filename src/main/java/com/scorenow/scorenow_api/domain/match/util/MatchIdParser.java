package com.scorenow.scorenow_api.domain.match.util;

public class MatchIdParser {

	private MatchIdParser() {
	}

	/** ID 규칙
	 * sportId: BETS + sportNo
	 * matchId: sportId + eventId
	 * playerId: sportId + playerApiId
	 */

	public static String extractEventId(String matchId, String sportId) {
		return matchId.substring(sportId.length()); // sportId(BETS1) 떼고 나머지
	}

	/** playerId: sportId + playerApiId */
	public static String extractSportIdFromPlayerId(String playerId) {
		if (playerId == null || !playerId.startsWith("BETS")) {
			throw new IllegalArgumentException("invalid playerId: " + playerId);
		}
		return playerId.replaceAll("\\d+$", "");
	}

	/** matchId 생성 */
	public static String buildMatchId(String sportId, String eventId) {
		if (sportId == null || eventId == null) {
			throw new IllegalArgumentException("sportId or eventId is null");
		}
		return sportId + eventId;
	}
}
