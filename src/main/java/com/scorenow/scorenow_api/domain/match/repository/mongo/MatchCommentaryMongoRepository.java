package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MatchCommentaryMongoRepository extends MongoRepository<MatchCommentaryDocument, String> {
    @Query(value = "{ 'matchId' : ?0, 'visible' : true }",
            sort = "{ 'created_at' :  -1 }")
    List<MatchCommentaryDocument> findVisibleCommentaries(Long matchId);}
