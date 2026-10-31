package com.scorenow.scorenow_api.domain.odds.repository;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchOddsDisplayPolicyRepository extends JpaRepository<MatchOddsDisplayPolicy, Long> {
    Optional<MatchOddsDisplayPolicy> findByMatchIdAndCategory(Long matchId, OddsCategory category);
    List<MatchOddsDisplayPolicy> findAllByMatchIdInAndCategory(Collection<Long> matchIds, OddsCategory category);
}
