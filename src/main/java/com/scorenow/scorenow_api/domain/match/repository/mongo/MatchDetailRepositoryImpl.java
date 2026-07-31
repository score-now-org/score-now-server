package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public List<MatchDetailDocument> findAllById(List<Long> matchIds) {
        return mongoRepository.findAllById(matchIds);
    }

    @Override
    public void upsertCurrentCommentary(Long matchId, String content, String commentaryId, boolean highlighted) {
        Query query = new Query(Criteria.where("_id").is(matchId));
        Update update = new Update()
                .set("currentCommentary", content)
                .set("currentCommentaryId", commentaryId)
                .set("currentCommentaryHighlighted", highlighted)
                .setOnInsert("_id", matchId);

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }

    @Override
    public void upsertMatchDetail(MatchDetailDocument matchDetailDocument) {
        Query query = new Query(Criteria.where("_id").is(matchDetailDocument.getId()));
        Update update = new Update();

        // 홈팀 골 및 스텟 정보
        update.set("homeScore", matchDetailDocument.getHomeScore());
        if (matchDetailDocument.getHomeShootOutScore() != null) {
            update.set("homeShootOutScore", matchDetailDocument.getHomeShootOutScore());
        }
        update.set("homeStats", matchDetailDocument.getHomeStats());

        // 어웨이팀 골 및 스텟 정보
        update.set("awayScore", matchDetailDocument.getAwayScore());
        if (matchDetailDocument.getAwayShootOutScore() != null) {
            update.set("awayShootOutScore", matchDetailDocument.getAwayShootOutScore());
        }
        update.set("awayStats", matchDetailDocument.getAwayStats());

        // 추가시간의 경우 기존에 저장한 전반 추가시간이나 후반 추가시간이 사라지면 안되기 때문에, 값을 확인하고 부분적으로 수정해주는 방식으로 한다.
        AdditionalTime additionalTime = matchDetailDocument.getAdditionalTime();
        if (additionalTime != null) {
            Integer firstHalf = additionalTime.getFirstHalf();
            Integer secondHalf = additionalTime.getSecondHalf();
            Integer extraFirstHalf = additionalTime.getExtraFirstHalf();
            Integer extraSecondHalf = additionalTime.getExtraSecondHalf();

            if (firstHalf != null) {
                update.set("additionalTime.firstHalf", firstHalf);
            }

            if (secondHalf != null) {
                update.set("additionalTime.secondHalf", secondHalf);
            }

            if (extraFirstHalf != null) {
                update.set("additionalTime.extraFirstHalf", extraFirstHalf);
            }

            if (extraSecondHalf != null) {
                update.set("additionalTime.extraSecondHalf", extraSecondHalf);
            }
        }

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }

    @Override
    public void upsertMatchClock(Long matchId, MatchClock matchClock) {
        Query query = new Query(Criteria.where("_id").is(matchId));
        Update update = new Update();

        update.set("matchClock", matchClock);
        update.setOnInsert("_id", matchId);

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }
}
