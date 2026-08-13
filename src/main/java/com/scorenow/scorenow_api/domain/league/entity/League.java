package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

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

    private String imageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TeamDisplayOrder teamDisplayOrder = TeamDisplayOrder.HOME_AWAY;

    @Enumerated(EnumType.STRING)
    private DataOrigin dataOrigin;  // 데이터 원천 (외부 API, 수동등록)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sport_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Sport sport;

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
        this.teamDisplayOrder = teamDisplayOrder != null ? teamDisplayOrder : TeamDisplayOrder.HOME_AWAY;
    }

    public void updateDataOrigin(DataOrigin dataOrigin) {
        this.dataOrigin = dataOrigin;
    }

    public boolean hasTeamDisplayOrder() {
        return teamDisplayOrder != null;
    }

    public TeamDisplayOrder resolveTeamDisplayOrder() {
        return teamDisplayOrder != null ? teamDisplayOrder : TeamDisplayOrder.HOME_AWAY;
    }

    public String resolveLeagueName() {
        if (StringUtils.hasText(kName)) {
            return kName;
        }
        if (StringUtils.hasText(eName)) {
            return eName;
        }
        if (StringUtils.hasText(sName)) {
            return sName;
        }
        return "";
    }
}
