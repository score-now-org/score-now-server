package com.scorenow.scorenow_api.domain.match.repository.jpa;


import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.match.entity.Match;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, String> {
    List<Match> findByStatusCode(MatchStatus statusCode);
}
