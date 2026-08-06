package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public void updateCurrentCommentary(Long matchId, String content, String commentaryId, boolean highlighted) {
        Query query = new Query(Criteria.where("_id").is(matchId));
        LocalDateTime now = LocalDateTime.now();

        Update update = new Update()
                .set("currentCommentary", content)
                .set("currentCommentaryId", commentaryId)
                .set("currentCommentaryHighlighted", highlighted)
                .set("updated_at", now);

        mongoTemplate.updateFirst(query, update, MatchDetailDocument.class);
    }

    @Override
    public void upsertMatchDetail(MatchDetailDocument matchDetailDocument) {
        Query query = new Query(Criteria.where("_id").is(matchDetailDocument.getId()));
        LocalDateTime now = LocalDateTime.now();

        Update update = new Update()
                .setOnInsert("created_at", now)
                .set("type", matchDetailDocument.getType())               // 종목 타입
                .set("sportDetail", matchDetailDocument.getSportDetail())  // 종목 별 상세 정보
                .set("updated_at", now);

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }

    @Override
    public void createInitialMatchDetailIfAbsent(Long matchId, SportDetailType type) {
        Query query = new Query(Criteria.where("_id").is(matchId));
        LocalDateTime now = LocalDateTime.now();

        Update update = new Update()
                .setOnInsert("_id", matchId)
                .setOnInsert("type", type)
                .setOnInsert("currentCommentaryHighlighted", false)    // boolean 타입이므로 기본값 세팅이 필요
                .setOnInsert("created_at", now)
                .setOnInsert("updated_at", now);

        mongoTemplate.upsert(query, update, MatchDetailDocument.class);
    }

}
