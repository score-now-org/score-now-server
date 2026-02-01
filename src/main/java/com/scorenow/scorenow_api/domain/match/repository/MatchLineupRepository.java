package com.scorenow.scorenow_api.domain.match.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;

public interface MatchLineupRepository extends MongoRepository<MatchLineupDocument, String> {
}
