package com.scorenow.scorenow_api.domain.odds.entity;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "match_odds_display_policy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_odds_display_policy_match_category",
                        columnNames = {"match_id", "category"}
                )
        }
)
public class MatchOddsDisplayPolicy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OddsCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "displayed_registration_type", nullable = false)
    private OddsRegistrationType displayedRegistrationType;

    public MatchOddsDisplayPolicy(Long matchId, OddsCategory category) {
        this.matchId = matchId;
        this.category = category;
        this.displayedRegistrationType = OddsRegistrationType.AUTOMATIC;
    }

    public void changeDisplayedRegistrationType(OddsRegistrationType displayedRegistrationType) {
        if (displayedRegistrationType == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배당률 노출 방식은 필수입니다.");
        }

        this.displayedRegistrationType = displayedRegistrationType;
    }
}
