package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leagues",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leagues",
                        columnNames = {"sport_id", "e_name"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class League extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_id")
    private Long sportId;

    @Column(name = "k_name")
    private String kName;

    @Column(name = "e_name")
    private String eName;

    @Column(name = "s_name")
    private String sName; // 숏네임

    private String cc;

    @Enumerated(EnumType.STRING)
    private TeamDisplayOrder teamDisplayOrder;

    public void updateSportId(Long sportId) {
        this.sportId = sportId;
    }

    public void updateEName(String eName) {
        this.eName = eName;
    }

    public void updateKName(String kName) {
        this.kName = kName;
    }

    public void updateSName(String sName) {
        this.sName = sName;
    }

    public void updateTeamDisplayOrder(TeamDisplayOrder teamDisplayOrder) {
        this.teamDisplayOrder = teamDisplayOrder;
    }

    public boolean hasTeamDisplayOrder() {
        return teamDisplayOrder != null;
    }
}