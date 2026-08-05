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

        update.set("homeScore", matchDetailDocument.getHomeScore());    // 홈팀 골
        update.set("awayScore", matchDetailDocument.getAwayScore());    // 어웨이팀 골
        update.set("sportDetail", matchDetailDocument.getSportDetail()); // 종목 별 상세 정보

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }

}
