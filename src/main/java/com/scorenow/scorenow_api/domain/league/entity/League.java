package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leagues")
@Getter @Setter
@NoArgsConstructor
public class League extends BaseEntity{
    @Id
    private String id; // BETS + sportId + leagueId
    private String sportId;
    private String kName;
    private String eName;
    private String sName; // 숏네임
}