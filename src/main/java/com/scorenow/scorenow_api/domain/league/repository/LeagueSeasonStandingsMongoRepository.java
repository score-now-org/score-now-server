package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeagueSeasonStandingsMongoRepository extends MongoRepository<LeagueSeasonStandingsDataDocument, String> {
    Optional<LeagueSeasonStandingsDataDocument> findByLeagueSeasonId(Long leagueSeasonId);

    void deleteByLeagueSeasonId(Long leagueSeasonId);

    List<LeagueSeasonStandingsDataDocument> findAllByLeagueSeasonStandingsIdIn(Collection<Long> leagueSeasonStandingsIds);
}
