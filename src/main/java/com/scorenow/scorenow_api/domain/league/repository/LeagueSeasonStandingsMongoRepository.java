package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeagueSeasonStandingsMongoRepository extends MongoRepository<LeagueSeasonStandingsDataDocument, String> {
    void deleteByLeagueSeasonId(Long leagueSeasonId);
}
