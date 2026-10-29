package com.scorenow.scorenow_api.domain.odds.value;

import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class OddsOrigin {

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false)
    private OddsRegistrationType registrationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "odds_source")
    private OddsSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "bookmaker")
    private OddsBookmaker bookmaker;

    private OddsOrigin(
            OddsRegistrationType registrationType,
            OddsSource source,
            OddsBookmaker bookmaker
    ) {
        validate(registrationType, source, bookmaker);
        this.registrationType = registrationType;
        this.source = source;
        this.bookmaker = bookmaker;
    }

    public static OddsOrigin manual() {
        return new OddsOrigin(OddsRegistrationType.MANUAL, null, null);
    }

    public static OddsOrigin automatic(OddsSource source, OddsBookmaker bookmaker) {
        return new OddsOrigin(OddsRegistrationType.AUTOMATIC, source, bookmaker);
    }

    public void validateCategory(OddsCategory category) {
        if (category == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "배당률 카테고리는 필수입니다.");
        }

        if (registrationType == OddsRegistrationType.MANUAL) {
            return;
        }

        boolean supported = switch (source) {
            case BETMAN -> category == OddsCategory.DOMESTIC;
            case BETS_API -> category == OddsCategory.OVERSEAS;
        };

        if (!supported) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "배당률 카테고리와 출처 조합이 올바르지 않습니다.");
        }
    }

    public boolean isAutomatic() {
        return registrationType == OddsRegistrationType.AUTOMATIC;
    }

    public boolean isManual() {
        return registrationType == OddsRegistrationType.MANUAL;
    }

    private static void validate(OddsRegistrationType registrationType, OddsSource source, OddsBookmaker bookmaker) {
        if (registrationType == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "배당률 등록 방식은 필수입니다.");
        }

        if (registrationType == OddsRegistrationType.MANUAL) {
            if (source != null || bookmaker != null) {
                throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "수동 배당률에는 출처와 북메이커를 지정할 수 없습니다.");
            }
            return;
        }

        if (source == null || bookmaker == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "자동 배당률에는 출처와 북메이커가 모두 필요합니다.");
        }

        boolean supported = switch (source) {
            case BETMAN -> bookmaker == OddsBookmaker.BETMAN;
            case BETS_API -> bookmaker == OddsBookmaker.BET365;
        };

        if (!supported) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_STATE, "지원하지 않는 배당률 출처와 북메이커 조합입니다.");
        }
    }
}
