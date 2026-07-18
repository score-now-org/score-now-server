package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "league_season_standings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_league_season_standings_league_season_id",
                        columnNames = "league_season_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LeagueSeasonStandings extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_season_id", nullable = false)
    private Long leagueSeasonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "standing_type", nullable = false)
    private LeagueSeasonStandingsType standingsType;

    @Column(name = "image_url")
    private String imageUrl;

    // JPA 관계 추가 (Fetch join용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_season_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private LeagueSeason leagueSeason;

    public static LeagueSeasonStandings from(LeagueSeason leagueSeason, LeagueSeasonStandingsType standingsType) {
        validateStandingsTypeAllowed(leagueSeason, standingsType);

        return LeagueSeasonStandings.builder()
                .leagueSeasonId(leagueSeason.getId())
                .standingsType(standingsType)
                .build();
    }

    public static void validateStandingsTypeAllowed(
            LeagueSeason leagueSeason,
            LeagueSeasonStandingsType standingsType
    ) {
        if (leagueSeason == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 시즌 정보는 필수입니다.");
        }

        if (standingsType == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 관리 타입은 필수입니다.");
        }

        League league = leagueSeason.getLeague();
        if (league == null) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }

        if (league.getDataOrigin() == DataOrigin.MANUAL && standingsType != LeagueSeasonStandingsType.IMAGE) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수동 관리 리그는 이미지 타입만 선택할 수 있습니다.");
        }
    }

    public void updateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미지 URL 은 필수입니다.");
        }
        this.imageUrl = imageUrl;
    }

    public void updateStandingsType(
            LeagueSeason leagueSeason,
            LeagueSeasonStandingsType newType
    ) {
        validateStandingsTypeAllowed(leagueSeason, newType);

        this.standingsType = newType;
    }
}
