package com.scorenow.scorenow_api.domain.match.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "featured_matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeaturedMatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private FeaturedMatchType type;

    @Column(name = "display_date")
    private LocalDate displayDate;

    @Column(name = "display_order")
    private Integer displayOrder;

    // JPA 관계 추가 (Fetch join용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Match match;

    public FeaturedMatch(Long matchId, LocalDate displayDate, FeaturedMatchType type, Integer displayOrder) {
        this.matchId = matchId;
        this.type = type;
        this.displayDate = displayDate;
        this.displayOrder = displayOrder;
    }

    public void updateDisplayOrder(int newDisplayOrder) {
        if (newDisplayOrder <= 0) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER, "순서가 0보다 작을 수 없습니다.");
        }

        this.displayOrder = newDisplayOrder;
    }

    public void moveUpDisplayOrder() {
        if (this.displayOrder <= 1) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER, "순서를 더 올릴 수 없습니다.");
        }

        this.displayOrder -= 1;
    }
}
