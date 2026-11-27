package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;
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
    void 저장된_축구_슈팅_통계를_재계산하지_않고_반환한다() {
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .type(SportCode.FOOTBALL)
                .sportDetail(FootballDetail.builder()
                        .homeStats(FootballStats.builder().shots(7).shotsOnTarget(5).build())
                        .awayStats(FootballStats.builder().shots(2).shotsOnTarget(3).build())
                        .build())
                .build();

        FootballStatisticsResponse response = (FootballStatisticsResponse) registry.resolve(matchDetail);

        assertThat(response.getHomeStats().getShots()).isEqualTo(7);
        assertThat(response.getHomeStats().getShotsOnTarget()).isEqualTo(5);
        assertThat(response.getAwayStats().getShots()).isEqualTo(2);
        assertThat(response.getAwayStats().getShotsOnTarget()).isEqualTo(3);
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
