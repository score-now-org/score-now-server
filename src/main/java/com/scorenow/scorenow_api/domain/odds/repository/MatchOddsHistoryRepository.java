package com.scorenow.scorenow_api.domain.odds.repository;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MatchOddsHistoryRepository extends JpaRepository<MatchOddsHistory, Long> {

    @Query("""
            select moh
            from MatchOddsHistory moh
            where moh.matchId = :matchId
              and moh.category = :category
              and moh.origin.registrationType = :registrationType
              and moh.marketType in :marketTypes
            """)
    Page<MatchOddsHistory> findByMatchIdAndCategoryAndRegistrationTypeAndMarketTypeIn(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            Set<OddsMarketType> marketTypes,
            Pageable pageable
    );

    @Query("""
            select moh
            from MatchOddsHistory moh
            where moh.id = :historyId
              and moh.matchId = :matchId
              and moh.category = :category
              and moh.origin.registrationType = :registrationType
            """)
    Optional<MatchOddsHistory> findByIdAndMatchIdAndCategoryAndRegistrationType(
            Long historyId,
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType
    );

    @Query("""
            select moh
            from MatchOddsHistory moh
            where moh.matchId = :matchId
              and moh.category = :category
              and moh.origin.registrationType = :registrationType
              and moh.status = :status
            """)
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
              and moh.origin.registrationType = :registrationType
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

    @Query(value = """
            select moh.*
            from match_odds_history moh
            join (
                select candidate.id,
                       row_number() over (
                           partition by candidate.match_id
                           order by candidate.published_at desc, candidate.id desc
                       ) as row_num
                from match_odds_history candidate
                where candidate.match_id in (:matchIds)
                  and candidate.category = :#{#category.name()}
                  and candidate.market_type = :#{#marketType.name()}
                  and candidate.registration_type = :#{#origin.registrationType.name()}
                  and candidate.odds_source <=> :#{#origin.source?.name()}
                  and candidate.bookmaker <=> :#{#origin.bookmaker?.name()}
                  and candidate.status = 'ACTIVE'
            ) ranked on ranked.id = moh.id
            where ranked.row_num <= 2
            order by moh.match_id, moh.published_at desc, moh.id desc
            """,
            nativeQuery = true
    )
    List<MatchOddsHistory> findLatestTwoActivePerMatch(
            @Param("matchIds") Collection<Long> matchIds,
            @Param("category") OddsCategory category,
            @Param("marketType") OddsMarketType marketType,
            @Param("origin") OddsOrigin origin
    );

    @Query(value = """
            select moh
            from MatchOddsHistory moh
            where moh.matchId = :matchId
              and moh.category = :category
              and moh.marketType = :marketType
              and moh.status = :status
              and moh.origin.registrationType = :registrationType
              and (
                       (:source is null and moh.origin.source is null)
                       or moh.origin.source = :source
                  )
              and (
                       (:bookmaker is null and moh.origin.bookmaker is null)
                       or moh.origin.bookmaker = :bookmaker
                  )
              and (
                       :cursorPublishedAt is null
                       or moh.publishedAt < :cursorPublishedAt
                       or (moh.publishedAt = :cursorPublishedAt and moh.id < :cursorHistoryId)
                   )
            order by moh.publishedAt desc, moh.id desc
            """)
    List<MatchOddsHistory> findActiveHistorySlice(
            @Param("matchId") Long matchId,
            @Param("category") OddsCategory category,
            @Param("marketType") OddsMarketType marketType,
            @Param("status") OddsStatus status,
            @Param("registrationType") OddsRegistrationType registrationType,
            @Param("source") OddsSource source,
            @Param("bookmaker") OddsBookmaker bookmaker,
            @Param("cursorPublishedAt") Instant cursorPublishedAt,
            @Param("cursorHistoryId") Long cursorHistoryId,
            Pageable pageable
    );
}
