package com.scorenow.scorenow_api.domain.league.mapper;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeagueSeasonStandingsMapper {
    public LeagueSeasonStandingsDataDocument toDocument(
            BetsStandingsResponse.Result result,
            LeagueSeasonStandingsSyncTarget syncTarget) {

        BetsStandingsResponse.Season externalSeason = result.getSeason();

        return LeagueSeasonStandingsDataDocument.builder()
                .leagueId(syncTarget.getLeagueId())
                .apiLeagueId(syncTarget.getApiLeagueId())
                .dataOrigin(syncTarget.getDataOrigin())
                .leagueSeasonId(syncTarget.getLeagueSeasonId())
                .externalSeasonName(externalSeason != null ? externalSeason.getName() : null)
                .externalSeasonStartAt(externalSeason != null ? externalSeason.getStartTime() : null)
                .externalSeasonEndAt(externalSeason != null ? externalSeason.getEndTime() : null)
                .standingsTable(toStandingsTable(result.getOverall()))
                .build();
    }

    private LeagueSeasonStandingsDataDocument.StandingsTable toStandingsTable(BetsStandingsResponse.Overall overall) {
        if (overall == null) {
            return null;
        }

        if (overall.getTables() == null || overall.getTables().isEmpty()) {
            return null;
        }

        BetsStandingsResponse.Table table = overall.getTables().get(0);

        return LeagueSeasonStandingsDataDocument.StandingsTable.builder()
                .name(table.getName())
                .groupName(table.getGroupName())
                .currentRound(table.getCurrentRound())
                .maxRounds(table.getMaxRounds())
                .rows(toStandingsRows(table.getRows()))
                .build();
    }

    private List<LeagueSeasonStandingsDataDocument.StandingsRow> toStandingsRows(List<BetsStandingsResponse.Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        return rows.stream()
                .map(this::toStandingRow)
                .toList();
    }

    private LeagueSeasonStandingsDataDocument.StandingsRow toStandingRow(BetsStandingsResponse.Row row) {
        return LeagueSeasonStandingsDataDocument.StandingsRow.builder()
                .position(row.getPos())
                .positionChange(row.getChange())
                .played(row.played())
                .win(row.safeWin())
                .draw(row.safeDraw())
                .loss(row.safeLoss())
                .goalsFor(row.safeGoalsFor())
                .goalsAgainst(row.safeGoalsAgainst())
                .goalDifference(row.goalDifference())
                .points(row.getPoints())
                .promotion(toPromotion(row.getPromotion()))
                .team(toTeam(row.getTeam()))
                .build();
    }

    private LeagueSeasonStandingsDataDocument.Promotion toPromotion(BetsStandingsResponse.Promotion promotion) {
        if (promotion == null) {
            return null;
        }

        return LeagueSeasonStandingsDataDocument.Promotion.builder()
                .name(promotion.getName())
                .shortName(promotion.getShortName())
                .build();
    }

    private LeagueSeasonStandingsDataDocument.Team toTeam(BetsStandingsResponse.Team team) {
        if (team == null) {
            return null;
        }

        return LeagueSeasonStandingsDataDocument.Team.builder()
                .apiTeamId(team.getId())
                .name(team.getName())
                .imageId(team.getImageId())
                .countryCode(team.getCc())
                .build();
    }
}
