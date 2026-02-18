package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface MatchCommentaryRepository extends MongoRepository<MatchCommentaryDocument, String> {
}