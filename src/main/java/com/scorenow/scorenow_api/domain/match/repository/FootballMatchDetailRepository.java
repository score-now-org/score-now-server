package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballAdditionalTime;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;

public interface FootballMatchDetailRepository {

    void updateAdditionalTime(Long matchId, FootballAdditionalTime additionalTime);

    void updateStats(Long matchId, FootballStats homeStats, FootballStats awayStats);

    void updateShootOutScore(Long matchId, FootballShootOutScore shootOutScore);

    void updateClock(Long matchId, FootballClock clock);
}
