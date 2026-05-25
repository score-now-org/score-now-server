package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument.*;

@Repository
@RequiredArgsConstructor
public class MatchDetailRepositoryImpl implements MatchDetailRepository {
    private final MatchDetailMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetailDocument) {
        return mongoRepository.save(matchDetailDocument);
    }

    @Override
    public Optional<MatchDetailDocument> findById(Long matchId) {
        return mongoRepository.findById(matchId);
    }

    @Override
    public long updateCurrentCommentary(Long matchId, String content, String commentaryId) {
        return mongoRepository.updateCurrentCommentary(matchId, content, commentaryId);
    }

    @Override
    public void upsertMatchDetail(MatchDetailDocument matchDetailDocument) {
        Query query = new Query(Criteria.where("_id").is(matchDetailDocument.getId()));
        Update update = new Update();

        update.set("homeScore", matchDetailDocument.getHomeScore());
        update.set("homeStats", matchDetailDocument.getHomeStats());
        update.set("awayScore", matchDetailDocument.getAwayScore());
        update.set("awayStats", matchDetailDocument.getAwayStats());

        ExtraTime extraTime = matchDetailDocument.getExtraTime();
        if (extraTime != null) {
            Integer firstHalf = extraTime.getFirstHalf();
            Integer secondHalf = extraTime.getSecondHalf();

            if (firstHalf != null) {
                update.set("extraTime.firstHalf", firstHalf);
            }

            if (secondHalf != null) {
                update.set("extraTime.secondHalf", secondHalf);
            }
        }

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }
}
