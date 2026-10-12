package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;
import com.scorenow.scorenow_api.domain.match.repository.BaseballMatchDetailRepository;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class BaseballMatchDetailRepositoryImpl implements BaseballMatchDetailRepository {
    private static final String SPORT_DETAIL_PATH = "sportDetail";

    private final MongoTemplate mongoTemplate;

    @Override
    public void updateCurrentHalfInning(Long matchId, BaseballHalfInning currentHalfInning, BaseballCount count, BaseballBaseStates baseStates) {
        updateBaseballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".currentHalfInning", currentHalfInning)
                .set(SPORT_DETAIL_PATH + ".count", count)
                .set(SPORT_DETAIL_PATH + ".baseStates", baseStates)
        );
    }

    @Override
    public void updateInningScores(Long matchId, BaseballInningScores scores) {
        updateBaseballDetail(matchId, new Update().set(SPORT_DETAIL_PATH + ".inningScores", scores));
    }

    @Override
    public void updateStats(Long matchId, BaseballStats homeStats, BaseballStats awayStats) {
        updateBaseballDetail(matchId, new Update()
                .set(SPORT_DETAIL_PATH + ".homeStats", homeStats)
                .set(SPORT_DETAIL_PATH + ".awayStats", awayStats));
    }

    @Override
    public void updateCount(Long matchId, BaseballCount count) {
        updateBaseballDetail(matchId, new Update().set(SPORT_DETAIL_PATH + ".count", count));
    }

    @Override
    public void updateDetailedBroadcastEnabled(Long matchId, boolean detailedBroadcastEnabled) {
        updateBaseballDetail(matchId, new Update().set(
                SPORT_DETAIL_PATH + ".detailedBroadcastEnabled",
                detailedBroadcastEnabled)
        );
    }

    @Override
    public void updateBaseStates(Long matchId, BaseballBaseStates baseStates) {
        updateBaseballDetail(matchId, new Update().set(
                SPORT_DETAIL_PATH + ".baseStates",
                baseStates)
        );
    }

    @Override
    public void updatePitcher(Long matchId, String pitcher) {
        updateBaseballDetail(matchId, new Update().set(SPORT_DETAIL_PATH + ".pitcher", pitcher));
    }

    @Override
    public void updateBatter(Long matchId, String batter) {
        updateBaseballDetail(matchId, new Update().set(SPORT_DETAIL_PATH + ".batter", batter));
    }

    private void updateBaseballDetail(Long matchId, Update update) {
        Query query = Query.query(Criteria.where("_id").is(matchId)
                .and("type").is(SportCode.BASEBALL));

        update.set(SPORT_DETAIL_PATH + "._class", BaseballDetail.TYPE_ALIAS)
                .set("updated_at", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, MatchDetailDocument.class);
    }
}
