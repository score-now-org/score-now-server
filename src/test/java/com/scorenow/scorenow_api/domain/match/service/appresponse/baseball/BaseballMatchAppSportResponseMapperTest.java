package com.scorenow.scorenow_api.domain.match.service.appresponse.baseball;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseballMatchAppSportResponseMapperTest {

    private final BaseballMatchAppSportResponseMapper mapper = new BaseballMatchAppSportResponseMapper();

    @Test
    void 진행중이고_상세중계가_활성화된_야구는_목록용_진루현황과_아웃카운트를_반환한다() {
        Match match = Match.builder()
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .detailedBroadcastEnabled(true)
                .baseStates(BaseballBaseStates.of(Map.of(
                        BaseballBase.FIRST_BASE,
                        new BaseballBaseStates.BaseballBaseState(true, "1루 주자")
                )))
                .count(new BaseballCount(2, 1, 1))
                .build();

        BaseballAppLiveSummaryResponse response =
                (BaseballAppLiveSummaryResponse) mapper.toLiveSummary(match, baseballDetail);

        assertThat(response.detailedBroadcastEnabled()).isTrue();
        assertThat(response.outs()).isEqualTo(1);
        assertThat(response.baseStates())
                .extracting(BaseballAppLiveSummaryResponse.BaseStateResponse::runnerOnBase)
                .containsExactly(true, false, false);
    }

    @Test
    void 상세중계가_비활성화된_야구는_목록용_라이브_요약을_반환하지_않는다() {
        Match match = Match.builder()
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .detailedBroadcastEnabled(false)
                .build();

        assertThat(mapper.toLiveSummary(match, baseballDetail)).isNull();
    }

    @Test
    void 진행중이_아닌_야구는_상세중계가_활성화되어도_목록용_라이브_요약을_반환하지_않는다() {
        Match match = Match.builder()
                .statusCode(MatchStatus.ENDED)
                .build();
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .detailedBroadcastEnabled(true)
                .build();

        assertThat(mapper.toLiveSummary(match, baseballDetail)).isNull();
    }

    @Test
    void 야구_상세응답은_상세중계_활성화와_무관하게_현재이닝과_이닝별점수를_반환한다() {
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.SECOND_INNING_TOP)
                .inningScores(BaseballInningScores.of(Map.of(
                        BaseballHalfInning.FIRST_INNING_TOP, 1,
                        BaseballHalfInning.FIRST_INNING_BOTTOM, 2
                )))
                .detailedBroadcastEnabled(false)
                .build();

        BaseballAppMatchDetailResponse response =
                (BaseballAppMatchDetailResponse) mapper.toDetail(baseballDetail);

        assertThat(response.currentInning().code()).isEqualTo("SECOND_INNING_TOP");
        assertThat(response.inningScores().get(0).homeScore()).isEqualTo(2);
        assertThat(response.inningScores().get(0).awayScore()).isEqualTo(1);
        assertThat(response.detailed()).isNull();
    }
}
