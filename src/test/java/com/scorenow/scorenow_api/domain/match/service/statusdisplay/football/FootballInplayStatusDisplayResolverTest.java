package com.scorenow.scorenow_api.domain.match.service.statusdisplay.football;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.football.FootballInplayStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.displaytext.football.FootballClockDisplayTextResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FootballInplayStatusDisplayResolverTest {

    private final FootballInplayStatusDisplayResolver resolver =
            new FootballInplayStatusDisplayResolver(new FootballClockDisplayTextResolver());

    @Test
    void REST_응답에서_실제_경과시간과_표시문구를_분리한다() {
        FootballClock clock = FootballClock.builder()
                .phase(FootballPhase.FIRST_HALF)
                .phaseElapsedSeconds(0)
                .clockSyncedAt(Instant.now())
                .running(false)
                .build();
        Match match = Match.builder()
                .id(1L)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        MatchDetailDocument detailDocument = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportCode.FOOTBALL)
                .sportDetail(FootballDetail.builder().clock(clock).build())
                .build();

        MatchStatusDisplayResponse response = resolver.resolve(match, detailDocument);
        FootballInplayStatusDisplayDetailResponse detail =
                (FootballInplayStatusDisplayDetailResponse) response.getDetail();

        assertThat(response.getDisplayText()).isEqualTo("전반 1");
        assertThat(detail.getPhaseElapsedMinutes()).isZero();
        assertThat(detail.getPhaseElapsedSeconds()).isZero();
    }
}
