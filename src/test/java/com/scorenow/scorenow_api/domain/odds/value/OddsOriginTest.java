package com.scorenow.scorenow_api.domain.odds.value;

import com.scorenow.scorenow_api.domain.odds.enums.OddsBookmaker;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsSource;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OddsOriginTest {

    @Test
    void 수동_배당률은_출처와_북메이커_없이_생성한다() {
        OddsOrigin origin = OddsOrigin.manual();

        assertThat(origin.getRegistrationType()).isEqualTo(OddsRegistrationType.MANUAL);
        assertThat(origin.getSource()).isNull();
        assertThat(origin.getBookmaker()).isNull();
        assertThat(origin.isManual()).isTrue();
        assertThat(origin.isAutomatic()).isFalse();
    }

    @Test
    void Bets_API와_Bet365_조합으로_해외_자동_배당률_출처를_생성한다() {
        OddsOrigin origin = OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);

        assertThat(origin.getRegistrationType()).isEqualTo(OddsRegistrationType.AUTOMATIC);
        assertThat(origin.getSource()).isEqualTo(OddsSource.BETS_API);
        assertThat(origin.getBookmaker()).isEqualTo(OddsBookmaker.BET365);
        assertThat(origin.isAutomatic()).isTrue();
        assertThatCode(() -> origin.validateCategory(OddsCategory.OVERSEAS))
                .doesNotThrowAnyException();
    }

    @Test
    void 배트맨_출처와_북메이커_조합으로_국내_자동_배당률_출처를_생성한다() {
        OddsOrigin origin = OddsOrigin.automatic(OddsSource.BETMAN, OddsBookmaker.BETMAN);

        assertThatCode(() -> origin.validateCategory(OddsCategory.DOMESTIC))
                .doesNotThrowAnyException();
    }

    @Test
    void 자동_배당률은_출처와_북메이커가_모두_필요하다() {
        assertThatThrownBy(() -> OddsOrigin.automatic(null, OddsBookmaker.BET365))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );

        assertThatThrownBy(() -> OddsOrigin.automatic(OddsSource.BETS_API, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );
    }

    @Test
    void 지원하지_않는_출처와_북메이커_조합은_생성할_수_없다() {
        assertThatThrownBy(() -> OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BETMAN))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );

        assertThatThrownBy(() -> OddsOrigin.automatic(OddsSource.BETMAN, OddsBookmaker.BET365))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );
    }

    @Test
    void 자동_배당률의_카테고리는_출처와_일치해야_한다() {
        OddsOrigin overseasOrigin = OddsOrigin.automatic(OddsSource.BETS_API, OddsBookmaker.BET365);
        OddsOrigin domesticOrigin = OddsOrigin.automatic(OddsSource.BETMAN, OddsBookmaker.BETMAN);

        assertThatThrownBy(() -> overseasOrigin.validateCategory(OddsCategory.DOMESTIC))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );
        assertThatThrownBy(() -> domesticOrigin.validateCategory(OddsCategory.OVERSEAS))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_STATE)
                );
    }
}
