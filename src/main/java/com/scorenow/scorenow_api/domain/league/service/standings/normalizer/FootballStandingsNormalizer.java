package com.scorenow.scorenow_api.domain.league.service.standings.normalizer;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.ExternalSeason;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData.*;

@Component
public class FootballStandingsNormalizer implements StandingsNormalizer {

    private static final String GROUP_KEY_SINGLE = "single";
    private static final String GROUP_KEY_PREFIX = "groupname:";
    private static final String GROUP_KEY_NULL = GROUP_KEY_PREFIX + "<null>";   // groupName 이 null 인 경우


    @Override
    public Long sportId() { //TODO: 추후 sportCode 도입 예정
        return 1L;
    }

    @Override
    public LeagueSeasonStandingsDataDocument normalize(
            BetsStandingsResponse.Result result,
            LeagueSeasonStandingsSyncTarget syncTarget) {

        if (result == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 결과는 필수입니다.");
        }

        FootballStandingsData data = toData(result.getOverall());

        // 리그 순위 정보가 없는 경우
        if (data == null || data.getGroups() == null || data.getGroups().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효한 리그 순위 정보가 없습니다.");
        }

        return LeagueSeasonStandingsDataDocument.builder()
                .sportId(syncTarget.getSportId())
                .leagueId(syncTarget.getLeagueId())
                .apiLeagueId(syncTarget.getApiLeagueId())
                .dataOrigin(syncTarget.getDataOrigin())
                .leagueSeasonId(syncTarget.getLeagueSeasonId())
                .leagueSeasonStandingsId(syncTarget.getLeagueSeasonStandingsId())
                .syncedAt(Instant.now())
                .data(data)
                .externalSeason(toExternalSeason(result.getSeason()))
                .groupMappings(toGroupMappings(data.getGroups()))
                .build();
    }

    private ExternalSeason toExternalSeason(BetsStandingsResponse.Season season) {
        if (season == null) {
            return null;
        }

        return ExternalSeason.builder()
                .name(season.getName())
                .startAt(season.getStartTime())
                .endAt(season.getEndTime())
                .build();
    }

    private FootballStandingsData toData(BetsStandingsResponse.Overall overall) {
        if (overall == null) {
            return null;
        }

        return FootballStandingsData.builder()
                .groups(toGroups(overall.getTables()))
                .build();
    }

    private List<StandingsGroup> toGroups(List<BetsStandingsResponse.Table> tables) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }

        List<StandingsGroup> groups = tables.stream()
                .map(table -> StandingsGroup.builder()
                        .groupKey(generateGroupKey(table, tables.size()))
                        .externalName(table.getName())
                        .externalGroupName(table.getGroupName())
                        .currentRound(table.getCurrentRound())
                        .maxRounds(table.getMaxRounds())
                        .standings(toStandings(table.getRows()))
                        .build())
                .toList();

        Set<String> uniqueKeys = groups.stream()
                .map(StandingsGroup::getGroupKey)
                .collect(Collectors.toSet());

        // 중복되는 그룹이 존재하는 경우
        if (uniqueKeys.size() != groups.size()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "순위 그룹 key가 중복되었습니다.");
        }

        return groups;
    }

    private boolean isGroupNameDuplicated(List<BetsStandingsResponse.Table> tables) {
        int groupCount = tables.size();

        Set<String> uniqueGroupNames = tables.stream()
                .map(BetsStandingsResponse.Table::getGroupName)
                .collect(Collectors.toSet());

        return groupCount != uniqueGroupNames.size();
    }

    private String generateGroupKey(BetsStandingsResponse.Table table, int groupCount) {

        // 그룹의 수가 1개인 경우
        if (groupCount == 1) {
            return GROUP_KEY_SINGLE;
        }

        // 그룹의 수가 2개 이상인 경우 + 현재 그룹명이 null 인 경우
        if (table.getGroupName() == null || table.getGroupName().isBlank()) {
            return GROUP_KEY_NULL;
        }

        String normalizedGroupName = table.getGroupName().trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        return GROUP_KEY_PREFIX + normalizedGroupName;
    }

    private List<Standing> toStandings(List<BetsStandingsResponse.Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        return rows.stream()
                .map(row -> Standing.builder()
                        .externalTeamId(row.getTeam() != null ? row.getTeam().getId() : null)
                        .externalTeamName(row.getTeam() != null ? row.getTeam().getName() : null)
                        .rank(row.getPos())
                        .rankOrder(row.getSortPos())
                        .played(row.played())
                        .win(row.getWin())
                        .draw(row.getDraw())
                        .loss(row.getLoss())
                        .goalsFor(row.getGoalsFor())
                        .goalsAgainst(row.getGoalsAgainst())
                        .goalDifference(row.goalDifference())
                        .points(row.getPoints())
                        .externalPromotion(toExternalPromotion(row.getPromotion()))
                        .build())
                .toList();
    }

    private ExternalPromotion toExternalPromotion(BetsStandingsResponse.Promotion promotion) {
        if (promotion == null) {
            return null;
        }

        return ExternalPromotion.builder()
                .name(promotion.getName())
                .shortName(promotion.getShortName())
                .build();
    }

    private List<LeagueSeasonStandingsDataDocument.GroupMapping> toGroupMappings(
            List<StandingsGroup> groups) {

        return groups.stream()
                .map(group -> LeagueSeasonStandingsDataDocument.GroupMapping.builder()
                        .groupKey(group.getGroupKey())
                        .build())
                .toList();
    }

}
