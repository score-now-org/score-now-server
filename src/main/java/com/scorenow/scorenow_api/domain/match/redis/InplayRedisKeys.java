package com.scorenow.scorenow_api.domain.match.redis;

public class InplayRedisKeys {
	private InplayRedisKeys() {
	}

	public static final String SEEN_PREFIX = "inplay:seen:";
	public static final String Q_NEW_LINEUP = "queue:new_inplay:lineup";
	public static final String LOCK_LINEUP_PREFIX = "lock:lineup:";
	public static final String DLQ_LINEUP_INIT = "dlq:lineup:init";
}

