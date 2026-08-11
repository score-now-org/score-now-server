package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeagueSeasonStandingsMongoRepository extends MongoRepository<LeagueSeasonStandingsDataDocument, String> {
    Optional<LeagueSeasonStandingsDataDocument> findByLeagueSeasonId(Long leagueSeasonId);

    void deleteByLeagueSeasonId(Long leagueSeasonId);
}
