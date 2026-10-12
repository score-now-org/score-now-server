package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;

public interface BaseballMatchDetailRepository {
    void updateInningScores(Long matchId, BaseballInningScores scores);

    void updateCurrentHalfInning(Long matchId, BaseballHalfInning currentHalfInning, BaseballCount count, BaseballBaseStates baseStates);

    void updateStats(Long matchId, BaseballStats homeStats, BaseballStats awayStats);

    void updateCount(Long matchId, BaseballCount count);

    void updateDetailedBroadcastEnabled(Long matchId, boolean detailedBroadcastEnabled);

    void updateBaseStates(Long matchId, BaseballBaseStates baseStates);

    void updatePitcher(Long matchId, String pitcher);

    void updateBatter(Long matchId, String batter);
}
