package com.scorenow.scorenow_api.domain.odds.entity;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.value.HandicapLine;
import com.scorenow.scorenow_api.domain.odds.value.OddsValue;
import com.scorenow.scorenow_api.domain.odds.value.UnderOverLine;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TODO: 자동/라이브 배당 이력 증가 전 조회 성능을 위해
 *      (match_id, category, registration_type, published_at) 복합 인덱스 추가를 검토한다.
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "match_odds_history")
public class MatchOddsHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OddsCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    private OddsMarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false)
    private OddsRegistrationType registrationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OddsStatus status;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "home_odds", precision = 10, scale = 2))
    private OddsValue homeOdds;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "draw_odds", precision = 10, scale = 2))
    private OddsValue drawOdds;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "away_odds", precision = 10, scale = 2))
    private OddsValue awayOdds;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "handicap_line", precision = 10, scale = 2))
    private HandicapLine handicapLine;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "under_over_line", precision = 10, scale = 2))
    private UnderOverLine underOverLine;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "under_odds", precision = 10, scale = 2))
    private OddsValue underOdds;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "over_odds", precision = 10, scale = 2))
    private OddsValue overOdds;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    public static MatchOddsHistory createWinDrawLoss(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            BigDecimal homeOdds,
            BigDecimal drawOdds,
            BigDecimal awayOdds,
            Instant publishedAt
    ) {
        MatchOddsHistory history = initialize(
                matchId,
                category,
                OddsMarketType.WIN_DRAW_LOSS,
                registrationType,
                publishedAt
        );

        history.homeOdds = OddsValue.from(homeOdds);
        history.drawOdds = drawOdds == null ? null : OddsValue.from(drawOdds);
        history.awayOdds = OddsValue.from(awayOdds);

        return history;
    }

    public static MatchOddsHistory createHandicap(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            BigDecimal homeOdds,
            BigDecimal handicapLine,
            BigDecimal awayOdds,
            Instant publishedAt
    ) {
        MatchOddsHistory history = initialize(
                matchId,
                category,
                OddsMarketType.HANDICAP,
                registrationType,
                publishedAt
        );

        history.homeOdds = OddsValue.from(homeOdds);
        history.awayOdds = OddsValue.from(awayOdds);
        history.handicapLine = HandicapLine.from(handicapLine);

        return history;
    }

    public static MatchOddsHistory createUnderOver(
            Long matchId,
            OddsCategory category,
            OddsRegistrationType registrationType,
            BigDecimal underOdds,
            BigDecimal underOverLine,
            BigDecimal overOdds,
            Instant publishedAt
    ) {
        MatchOddsHistory history = initialize(
                matchId,
                category,
                OddsMarketType.UNDER_OVER,
                registrationType,
                publishedAt
        );

        history.underOverLine = UnderOverLine.from(underOverLine);
        history.underOdds = OddsValue.from(underOdds);
        history.overOdds = OddsValue.from(overOdds);

        return history;
    }

    private static MatchOddsHistory initialize(
            Long matchId,
            OddsCategory category,
            OddsMarketType marketType,
            OddsRegistrationType registrationType,
            Instant publishedAt
    ) {
        MatchOddsHistory history = new MatchOddsHistory();
        history.matchId = matchId;
        history.category = category;
        history.marketType = marketType;
        history.registrationType = registrationType;
        history.status = OddsStatus.ACTIVE;
        history.publishedAt = publishedAt;
        return history;
    }

    public void invalidate() {
        this.status = OddsStatus.INVALIDATED;
    }
}
