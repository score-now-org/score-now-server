package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.entity.Match;

public interface MatchRepositoryCustom {
	Page<Match> searchMatches(MatchSearchCondition condition, Pageable pageable);
	Optional<Match> findByIdWithRelations(String id);
}
