package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "league_seasons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_league_seasons",
                        columnNames = {"league_id", "season_name"}
                )
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LeagueSeason extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "season_name", nullable = false)
    private String seasonName;

    @Column(name = "season_start_at", nullable = false)
    private LocalDate seasonStartAt;    // 관리자 페이지에서 한국 시간 기준으로 등록하기 때문에 KST 로 관리한다.

    @Column(name = "season_end_at", nullable = false)
    private LocalDate seasonEndAt;      // 관리자 페이지에서 한국 시간 기준으로 등록하기 때문에 KST 로 관리한다.

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    // soft-delete (LeagueSeason 은 순위 뿐만 아니라 시즌 별 선수 스텟에도 관여할 수 있기 때문에 hard-delete 를 하지 않는다.)
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // JPA 관계 추가 (Fetch join용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private League league;

    public static LeagueSeason create(
            Long leagueId,
            String seasonName,
            LocalDate seasonStartAt,
            LocalDate seasonEndAt,
            boolean isCurrent
    ) {
        if (leagueId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 ID 는 필수입니다.");
        }

        validateSeasonName(seasonName);
        validatePeriod(seasonStartAt, seasonEndAt);

        LeagueSeason leagueSeason = new LeagueSeason();
        leagueSeason.leagueId = leagueId;
        leagueSeason.seasonName = seasonName;
        leagueSeason.seasonStartAt = seasonStartAt;
        leagueSeason.seasonEndAt = seasonEndAt;
        leagueSeason.isCurrent = isCurrent;

        return leagueSeason;
    }

    public void updateSeasonName(String seasonName) {
        validateSeasonName(seasonName);

        this.seasonName = seasonName;
    }

    public void updatePeriod(LocalDate seasonStartAt, LocalDate seasonEndAt) {
        validatePeriod(seasonStartAt, seasonEndAt);

        this.seasonStartAt = seasonStartAt;
        this.seasonEndAt = seasonEndAt;
    }

    public void updateCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public void deactivate() {
        this.isActive = false;
        this.isCurrent = false;
    }

    public boolean isSameId(Long leagueSeasonId) {
        return this.id != null && this.id.equals(leagueSeasonId);
    }

    public boolean hasSameSeasonName(String seasonName) {
        return this.seasonName.equals(seasonName);
    }

    public boolean overlapsWith(LocalDate startAt, LocalDate endAt) {
        return !this.getSeasonStartAt().isAfter(endAt) && !this.getSeasonEndAt().isBefore(startAt);
    }

    private static void validateSeasonName(String seasonName) {
        if (seasonName == null || seasonName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌명은 필수입니다.");
        }
    }

    private static void validatePeriod(LocalDate seasonStartAt, LocalDate seasonEndAt) {
        if (seasonStartAt == null || seasonEndAt == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌 시작일과 종료일은 필수입니다.");
        }

        if (seasonStartAt.isAfter(seasonEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌 종료일이 시즌 시작일보다 과거일 수 없습니다.");
        }
    }
}
