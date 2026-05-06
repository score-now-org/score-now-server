package com.scorenow.scorenow_api.domain.match.repository.mongo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;

public interface MatchLineupRepository extends MongoRepository<MatchLineupDocument, Long> {

	List<MatchLineupDocument> findByIdIn(Collection<Long> ids);
}
