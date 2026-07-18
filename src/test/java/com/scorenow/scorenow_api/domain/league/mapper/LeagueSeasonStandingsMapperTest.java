package com.scorenow.scorenow_api.domain.league.mapper;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueSeasonStandingsMapperTest {

    private final LeagueSeasonStandingsMapper mapper = new LeagueSeasonStandingsMapper();

    @Test
    void 외부_순위_응답을_Mongo_문서로_변환한다() {
        BetsStandingsResponse.Result result = result(
                season("2025/26", 1_754_006_400L, 1_779_897_600L),
                overall(table(List.of(row(
                        1,
                        0,
                        10,
                        5,
                        3,
                        30,
                        12,
                        35,
                        promotion("Champions League", "CL"),
                        team("101", "Liverpool", "liverpool.png", "gb")
                ))))
        );

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(result, syncTarget());

        assertThat(document.getLeagueId()).isEqualTo(1L);
        assertThat(document.getApiLeagueId()).isEqualTo("94");
        assertThat(document.getDataOrigin()).isEqualTo(DataOrigin.BETS);
        assertThat(document.getLeagueSeasonId()).isEqualTo(10L);
        assertThat(document.getExternalSeasonName()).isEqualTo("2025/26");
        assertThat(document.getExternalSeasonStartAt()).isEqualTo(1_754_006_400L);
        assertThat(document.getExternalSeasonEndAt()).isEqualTo(1_779_897_600L);

        LeagueSeasonStandingsDataDocument.StandingsTable standingsTable = document.getStandingsTable();
        assertThat(standingsTable.getName()).isEqualTo("overall");
        assertThat(standingsTable.getRows()).hasSize(1);

        LeagueSeasonStandingsDataDocument.StandingsRow standingsRow = standingsTable.getRows().get(0);
        assertThat(standingsRow.getPosition()).isEqualTo(1);
        assertThat(standingsRow.getPlayed()).isEqualTo(18);
        assertThat(standingsRow.getWin()).isEqualTo(10);
        assertThat(standingsRow.getDraw()).isEqualTo(5);
        assertThat(standingsRow.getLoss()).isEqualTo(3);
        assertThat(standingsRow.getGoalDifference()).isEqualTo(18);
        assertThat(standingsRow.getPoints()).isEqualTo(35);
        assertThat(standingsRow.getPromotion().getShortName()).isEqualTo("CL");
        assertThat(standingsRow.getTeam().getApiTeamId()).isEqualTo("101");
    }

    @Test
    void 외부_시즌이_null이어도_문서로_변환한다() {
        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(null, overall(table(List.of()))),
                syncTarget()
        );

        assertThat(document.getExternalSeasonName()).isNull();
        assertThat(document.getExternalSeasonStartAt()).isNull();
        assertThat(document.getExternalSeasonEndAt()).isNull();
    }

    @Test
    void overall이_null이면_standingsTable은_null이다() {
        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), null),
                syncTarget()
        );

        assertThat(document.getStandingsTable()).isNull();
    }

    @Test
    void tables가_비어있으면_standingsTable은_null이다() {
        BetsStandingsResponse.Overall overall = new BetsStandingsResponse.Overall();
        overall.setTables(List.of());

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall),
                syncTarget()
        );

        assertThat(document.getStandingsTable()).isNull();
    }

    @Test
    void tables_내부_null_요소는_무시하고_첫번째_유효한_table을_변환한다() {
        BetsStandingsResponse.Table table = table(List.of(row(
                1,
                0,
                10,
                5,
                3,
                30,
                12,
                35,
                null,
                team("101", "Liverpool", "liverpool.png", "gb")
        )));
        BetsStandingsResponse.Overall overall = new BetsStandingsResponse.Overall();
        overall.setTables(Arrays.asList(null, table));

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall),
                syncTarget()
        );

        assertThat(document.getStandingsTable().getRows()).hasSize(1);
        assertThat(document.getStandingsTable().getRows().get(0).getTeam().getApiTeamId()).isEqualTo("101");
    }

    @Test
    void tables_내부에_null만_있으면_standingsTable은_null이다() {
        BetsStandingsResponse.Overall overall = new BetsStandingsResponse.Overall();
        overall.setTables(Arrays.asList(null, null));

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall),
                syncTarget()
        );

        assertThat(document.getStandingsTable()).isNull();
    }

    @Test
    void rows가_null이면_빈_리스트로_변환한다() {
        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall(table(null))),
                syncTarget()
        );

        assertThat(document.getStandingsTable().getRows()).isEmpty();
    }

    @Test
    void rows_내부_null_요소는_무시한다() {
        BetsStandingsResponse.Row row = row(
                1,
                0,
                10,
                5,
                3,
                30,
                12,
                35,
                null,
                team("101", "Liverpool", "liverpool.png", "gb")
        );

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall(table(Arrays.asList(null, row)))),
                syncTarget()
        );

        assertThat(document.getStandingsTable().getRows()).hasSize(1);
        assertThat(document.getStandingsTable().getRows().get(0).getTeam().getApiTeamId()).isEqualTo("101");
    }

    @Test
    void row의_optional_필드가_null이어도_변환한다() {
        BetsStandingsResponse.Row row = row(
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(
                result(season("2025/26", 1L, 2L), overall(table(List.of(row)))),
                syncTarget()
        );

        LeagueSeasonStandingsDataDocument.StandingsRow standingsRow =
                document.getStandingsTable().getRows().get(0);

        assertThat(standingsRow.getPlayed()).isZero();
        assertThat(standingsRow.getWin()).isZero();
        assertThat(standingsRow.getDraw()).isZero();
        assertThat(standingsRow.getLoss()).isZero();
        assertThat(standingsRow.getGoalDifference()).isZero();
        assertThat(standingsRow.getPromotion()).isNull();
        assertThat(standingsRow.getTeam()).isNull();
    }

    @Test
    void result가_null이면_변환에_실패한다() {
        assertThatThrownBy(() -> mapper.toDocument(null, syncTarget()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    private LeagueSeasonStandingsSyncTarget syncTarget() {
        return LeagueSeasonStandingsSyncTarget.builder()
                .leagueSeasonStandingsId(100L)
                .leagueSeasonId(10L)
                .leagueId(1L)
                .dataOrigin(DataOrigin.BETS)
                .apiLeagueId("94")
                .build();
    }

    private BetsStandingsResponse.Result result(
            BetsStandingsResponse.Season season,
            BetsStandingsResponse.Overall overall
    ) {
        BetsStandingsResponse.Result result = new BetsStandingsResponse.Result();
        result.setSeason(season);
        result.setOverall(overall);
        return result;
    }

    private BetsStandingsResponse.Season season(String name, Long startTime, Long endTime) {
        BetsStandingsResponse.Season season = new BetsStandingsResponse.Season();
        season.setName(name);
        season.setStartTime(startTime);
        season.setEndTime(endTime);
        return season;
    }

    private BetsStandingsResponse.Overall overall(BetsStandingsResponse.Table table) {
        BetsStandingsResponse.Overall overall = new BetsStandingsResponse.Overall();
        overall.setTables(List.of(table));
        return overall;
    }

    private BetsStandingsResponse.Table table(List<BetsStandingsResponse.Row> rows) {
        BetsStandingsResponse.Table table = new BetsStandingsResponse.Table();
        table.setName("overall");
        table.setGroupName("A");
        table.setCurrentRound(10);
        table.setMaxRounds(38);
        table.setRows(rows);
        return table;
    }

    private BetsStandingsResponse.Row row(
            Integer position,
            Integer positionChange,
            Integer win,
            Integer draw,
            Integer loss,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer points,
            BetsStandingsResponse.Promotion promotion,
            BetsStandingsResponse.Team team
    ) {
        BetsStandingsResponse.Row row = new BetsStandingsResponse.Row();
        row.setPos(position);
        row.setChange(positionChange);
        row.setWin(win);
        row.setDraw(draw);
        row.setLoss(loss);
        row.setGoalsFor(goalsFor);
        row.setGoalsAgainst(goalsAgainst);
        row.setPoints(points);
        row.setPromotion(promotion);
        row.setTeam(team);
        return row;
    }

    private BetsStandingsResponse.Promotion promotion(String name, String shortName) {
        BetsStandingsResponse.Promotion promotion = new BetsStandingsResponse.Promotion();
        promotion.setName(name);
        promotion.setShortName(shortName);
        return promotion;
    }

    private BetsStandingsResponse.Team team(String id, String name, String imageId, String countryCode) {
        BetsStandingsResponse.Team team = new BetsStandingsResponse.Team();
        team.setId(id);
        team.setName(name);
        team.setImageId(imageId);
        team.setCc(countryCode);
        return team;
    }
}
