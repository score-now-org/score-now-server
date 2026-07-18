package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueSeasonStandingsTest {

    @Test
    void 수동_관리_리그는_이미지_타입으로_순위_관리를_생성한다() {
        LeagueSeason leagueSeason = season(manualLeague(1L));

        LeagueSeasonStandings standings = LeagueSeasonStandings.from(
                leagueSeason,
                LeagueSeasonStandingsType.IMAGE
        );

        assertThat(standings.getLeagueSeasonId()).isEqualTo(10L);
        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
    }

    @Test
    void 수동_관리_리그는_외부_데이터_타입으로_순위_관리를_생성할_수_없다() {
        LeagueSeason leagueSeason = season(manualLeague(1L));

        assertThatThrownBy(() -> LeagueSeasonStandings.from(
                leagueSeason,
                LeagueSeasonStandingsType.EXTERNAL_DATA
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 외부_연동_리그는_외부_데이터_타입으로_변경할_수_있다() {
        LeagueSeason leagueSeason = season(betsLeague(1L));
        LeagueSeasonStandings standings = LeagueSeasonStandings.from(
                leagueSeason,
                LeagueSeasonStandingsType.IMAGE
        );

        standings.updateStandingsType(leagueSeason, LeagueSeasonStandingsType.EXTERNAL_DATA);

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.EXTERNAL_DATA);
    }

    @Test
    void 수동_관리_리그는_외부_데이터_타입으로_변경할_수_없다() {
        LeagueSeason leagueSeason = season(manualLeague(1L));
        LeagueSeasonStandings standings = LeagueSeasonStandings.from(
                leagueSeason,
                LeagueSeasonStandingsType.IMAGE
        );

        assertThatThrownBy(() -> standings.updateStandingsType(
                leagueSeason,
                LeagueSeasonStandingsType.EXTERNAL_DATA
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        assertThat(standings.getStandingsType()).isEqualTo(LeagueSeasonStandingsType.IMAGE);
    }

    @Test
    void 순위_관리_타입이_null이면_변경할_수_없다() {
        LeagueSeason leagueSeason = season(betsLeague(1L));
        LeagueSeasonStandings standings = LeagueSeasonStandings.from(
                leagueSeason,
                LeagueSeasonStandingsType.IMAGE
        );

        assertThatThrownBy(() -> standings.updateStandingsType(leagueSeason, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    private LeagueSeason season(League league) {
        LeagueSeason leagueSeason = LeagueSeason.create(
                league.getId(),
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        );

        ReflectionTestUtils.setField(leagueSeason, "id", 10L);
        ReflectionTestUtils.setField(leagueSeason, "league", league);

        return leagueSeason;
    }

    private League manualLeague(Long id) {
        return League.builder()
                .id(id)
                .kName("수동 리그")
                .dataOrigin(DataOrigin.MANUAL)
                .build();
    }

    private League betsLeague(Long id) {
        return League.builder()
                .id(id)
                .kName("외부 리그")
                .dataOrigin(DataOrigin.BETS)
                .build();
    }
}
