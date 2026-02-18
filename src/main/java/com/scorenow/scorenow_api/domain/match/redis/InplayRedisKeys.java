package com.scorenow.scorenow_api.domain.match.redis;

public class InplayRedisKeys {
	private InplayRedisKeys() {
	}

	public static final String SEEN_PREFIX = "inplay:seen:";

	public static final String Q_NEW_LINEUP = "queue:new_inplay:lineup";
	public static final String Q_NEW_DETAIL = "queue:new_inplay:detail";

	public static final String DUE_GOALS = "due:goals";
	public static final String DUE_DETAIL = "due:detail";

	public static final String LOCK_GOALS_PREFIX = "lock:goals:";   // + matchId
	public static final String LOCK_DETAIL_PREFIX = "lock:detail:"; // + matchId
	public static final String LOCK_LINEUP_PREFIX = "lock:lineup:"; // + matchId
}

