package com.scorenow.scorenow_api.domain.odds.entity;

import com.scorenow.scorenow_api.domain.odds.enums.OddsProvider;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "")
public class MatchOddsDisplayPolicy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long matchId;

    private OddsProvider provider;

    private OddsRegistrationType registrationType;
}
