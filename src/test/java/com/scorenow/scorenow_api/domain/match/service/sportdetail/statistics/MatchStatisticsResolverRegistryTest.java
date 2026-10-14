package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.football.FootballStatisticsResponse;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchStatisticsResolverRegistryTest {

    private final MatchStatisticsResolverRegistry registry =
            new MatchStatisticsResolverRegistry(List.of(new FootballStatisticsResolver()));

    @Test
    void 등록된_축구_통계를_반환한다() {
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .type(SportCode.FOOTBALL)
                .sportDetail(FootballDetail.empty())
                .build();

        assertThat(registry.resolve(matchDetail))
                .isInstanceOf(FootballStatisticsResponse.class);
    }

    @Test
    void 통계를_지원하지_않는_종목은_MS001_예외를_반환한다() {
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .type(SportCode.BASEBALL)
                .sportDetail(BaseballDetail.empty())
                .build();

        assertThatThrownBy(() -> registry.resolve(matchDetail))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_STATISTICS_NOT_SUPPORTED));
    }
}
