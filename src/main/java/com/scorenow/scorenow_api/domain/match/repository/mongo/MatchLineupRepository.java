package com.scorenow.scorenow_api.domain.match.repository.mongo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;

public interface MatchLineupRepository extends MongoRepository<MatchLineupDocument, String> {

	List<MatchLineupDocument> findByIdIn(Collection<String> ids);
}
