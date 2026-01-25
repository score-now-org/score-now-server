package com.scorenow.scorenow_api.domain.match.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match extends BaseEntity {
    @Id
    private String id; // BETS + sportId + eventId (예: BETS111275660)

    private String leagueId; // BETS + sportId + leagueId
    private String sportId;
    private String homeId;   // BETS + sportId + teamId
    private String awayId;   // BETS + sportId + teamId

    @Enumerated(EnumType.STRING)
    private MatchStatus statusCode;


    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime startAt;

    @Column(length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'A'")
    private String matchType = "A"; // 기본 A로 세팅

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isManual;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isActive;

}

