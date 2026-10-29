package com.scorenow.scorenow_api.domain.odds.repository;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MatchOddsHistoryRepository extends JpaRepository<MatchOddsHistory, Long> {

    Page<MatchOddsHistory> findByMatchIdAndCategoryAndRegistrationTypeAndMarketTypeIn(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            Set<OddsMarketType> marketTypes,
            Pageable pageable
    );

    Optional<MatchOddsHistory> findByIdAndMatchIdAndCategoryAndRegistrationType(
            Long historyId,
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType
    );

    List<MatchOddsHistory> findAllByMatchIdAndCategoryAndRegistrationTypeAndStatus(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            OddsStatus status
    );

    @Query("""
            select moh
            from MatchOddsHistory moh
            where moh.matchId = :matchId
              and moh.category = :category
              and moh.registrationType = :registrationType
              and moh.marketType = :marketType
              and moh.status = :status
            order by moh.publishedAt desc, moh.id desc
            limit 1
            """
    )
    Optional<MatchOddsHistory> findLatestOddsByMarket(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            OddsMarketType marketType,
            OddsStatus status
    );
}
