package com.scorenow.scorenow_api.domain.match.constant;

import java.time.ZoneId;

public final class MatchConstants {
    private MatchConstants() {
    }

    public static final String SEOUL_TIME_ZONE = "Asia/Seoul";
    public static final ZoneId SEOUL_TIME_ZONE_ID = ZoneId.of(SEOUL_TIME_ZONE);

    public static final String INPLAY_CANDIDATES_KEY = "inplay_candidates";
    public static final long INPLAY_CANDIDATES_SCAN_INTERVAL_HOURS = 1L;
    public static final long INPLAY_CANDIDATE_SCAN_LOOKBACK_MINUTES = 15L;

    public static final long MAX_GRACE_PERIOD_MINUTES = 15L;
}
