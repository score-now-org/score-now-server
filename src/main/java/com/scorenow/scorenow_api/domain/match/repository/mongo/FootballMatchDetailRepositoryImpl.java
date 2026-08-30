package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballAdditionalTime;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;
import com.scorenow.scorenow_api.domain.match.repository.FootballMatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class FootballMatchDetailRepositoryImpl implements FootballMatchDetailRepository {

    private static final String SPORT_DETAIL_PATH = "sportDetail";

    private final MongoTemplate mongoTemplate;

    @Override
    public void updateAdditionalTime(Long matchId, FootballAdditionalTime additionalTime) {
        updateFootballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".additionalTime", additionalTime));
    }

    @Override
    public void updateStats(Long matchId, FootballStats homeStats, FootballStats awayStats) {
        updateFootballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".homeStats", homeStats)
                .set(SPORT_DETAIL_PATH + ".awayStats", awayStats));
    }

    @Override
    public void updateShootOutScore(Long matchId, FootballShootOutScore shootOutScore) {
        updateFootballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".shootOutScore", shootOutScore));
    }

    @Override
    public void updateClock(Long matchId, FootballClock clock) {
        updateFootballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".clock", clock));
    }

    private void updateFootballDetail(Long matchId, Update update) {
        Query query = Query.query(Criteria.where("_id").is(matchId)
                .and("type").is(SportDetailType.FOOTBALL));

        update.set(SPORT_DETAIL_PATH + "._class", FootballDetail.TYPE_ALIAS)
                .set("updated_at", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, MatchDetailDocument.class);
    }
}
