package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsDisplayPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchOddsDisplayPolicyInitializationService {

    private final MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository;

    /**
     * 경기 생성 시 배당률 표기 정책 초기화
     */
    public void initialize(Long matchId) {
        List<MatchOddsDisplayPolicy> policies = List.of(
                new MatchOddsDisplayPolicy(matchId, OddsCategory.DOMESTIC),
                new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS)
        );

        matchOddsDisplayPolicyRepository.saveAll(policies);
    }
}
