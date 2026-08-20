package com.scorenow.scorenow_api.domain.team.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
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
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TeamType type;    // 구분 (국가대표, 클럽)

    private String kName;
    private String eName;
    private String sName; // 숏네임

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sport_id",
            nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Sport sport;

    private String cc; // 국가코드
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_origin", nullable = false, updatable = false)
    private DataOrigin dataOrigin;    // 데이터 원천 (외부 API, 수동등록)

    @Builder.Default
    private boolean isActive = true;

    public void updateType(TeamType teamType) {
        this.type = teamType;
    }

    public void updateKName(String kName) {
        this.kName = kName;
    }

    public void updateEName(String eName) {
        this.eName = eName;
    }

    public void updateSName(String sName) {
        this.sName = sName;
    }

    public void updateCountryCode(String cc) {
        this.cc = cc;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public String resolveTeamName() {
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
