package com.scorenow.scorenow_api.domain.odds.entity;

import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsProvider;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.value.HandicapLine;
import com.scorenow.scorenow_api.domain.odds.value.OddsValue;
import com.scorenow.scorenow_api.domain.odds.value.UnderOverLine;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "match_odds_history")
public class MatchOddsHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long matchId;

    @Enumerated(EnumType.STRING)
    private OddsProvider provider;

    @Enumerated(EnumType.STRING)
    private OddsMarketType marketType;

    @Enumerated(EnumType.STRING)
    private OddsRegistrationType registrationType;

    @Enumerated(EnumType.STRING)
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

    private Instant publishedAt;
}
