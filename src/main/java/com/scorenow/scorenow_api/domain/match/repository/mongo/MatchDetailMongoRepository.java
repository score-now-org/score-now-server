package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchDetailMongoRepository extends MongoRepository<MatchDetailDocument, Long> {
    @Query("{ '_id' : ?0 }")
    @Update("{'$set' : {'currentCommentary':?1 , 'currentCommentaryId':?2}}")
    long updateCurrentCommentary(Long matchId, String currentCommentary, String currentCommentaryId);
}