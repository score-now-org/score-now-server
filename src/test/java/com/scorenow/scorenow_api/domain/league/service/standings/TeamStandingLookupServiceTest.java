package com.scorenow.scorenow_api.domain.league.service.standings;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.extractor.TeamStandingExtractorRegistry;
import com.scorenow.scorenow_api.domain.league.service.standings.model.ExternalTeamStandingKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.LeagueTeamKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamStandingLookupServiceTest {

    @Mock
    private TeamExternalMappingRepository teamExternalMappingRepository;

    @Mock
    private LeagueSeasonRepository leagueSeasonRepository;

    @Mock
    private LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;

    @Mock
    private LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    @Mock
    private TeamStandingExtractorRegistry teamStandingExtractorRegistry;

    @InjectMocks
    private TeamStandingLookupService teamStandingLookupService;

    @Test
    void 외부_팀_매핑을_일괄_조회하고_경기_참가팀의_순위만_그룹_노출순서대로_반환한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        LeagueSeason leagueSeason = org.mockito.Mockito.mock(LeagueSeason.class);
        given(leagueSeason.getId()).willReturn(100L);

        LeagueSeasonStandings leagueSeasonStandings = LeagueSeasonStandings.builder()
                .id(200L)
                .leagueSeasonId(100L)
                .standingsType(LeagueSeasonStandingsType.EXTERNAL_DATA)
                .build();
        LeagueSeasonStandingsDataDocument document = LeagueSeasonStandingsDataDocument.builder()
                .leagueId(2L)
                .sportCode(SportCode.FOOTBALL)
                .dataOrigin(DataOrigin.BETS)
                .leagueSeasonStandingsId(200L)
                .groupMappings(List.of(
                        groupMapping("conference-west", "서부", 2),
                        groupMapping("overall", "통합", 1)))
                .build();

        ExternalTeamStandingKey requestedTeamKey = new ExternalTeamStandingKey(2L, DataOrigin.BETS, "external-10");
        ExternalTeamStandingKey unrelatedTeamKey = new ExternalTeamStandingKey(2L, DataOrigin.BETS, "external-30");

        given(leagueSeasonRepository.findAllByLeagueIdAndDate(Set.of(2L), date))
                .willReturn(List.of(leagueSeason));
        given(leagueSeasonStandingsRepository.findAllByLeagueSeasonIds(List.of(100L)))
                .willReturn(List.of(leagueSeasonStandings));
        given(leagueSeasonStandingsMongoRepository.findAllByLeagueSeasonStandingsIdIn(List.of(200L)))
                .willReturn(List.of(document));
        given(teamStandingExtractorRegistry.extract(eq(document), anyMap()))
                .willReturn(Map.of(
                        requestedTeamKey, List.of(
                                new TeamStandingSummary("서부", 2, 1),
                                new TeamStandingSummary("통합", 1, 3)),
                        unrelatedTeamKey, List.of(
                                new TeamStandingSummary("통합", 1, 8))));
        given(teamExternalMappingRepository.findAllByProviderAndApiTeamIdIn(
                eq(DataOrigin.BETS),
                eq(Set.of("external-10", "external-30"))))
                .willReturn(List.of(
                        TeamExternalMapping.of(DataOrigin.BETS, "external-10", 10L),
                        TeamExternalMapping.of(DataOrigin.BETS, "external-30", 30L)));

        Map<LeagueTeamKey, List<TeamStandingSummary>> result = teamStandingLookupService.getTeamStandings(
                Map.of(2L, Set.of(10L, 20L)),
                date);

        assertThat(result).containsOnlyKeys(new LeagueTeamKey(2L, 10L));
        assertThat(result.get(new LeagueTeamKey(2L, 10L)))
                .extracting(TeamStandingSummary::getGroupName)
                .containsExactly("통합", "서부");
        verify(teamExternalMappingRepository).findAllByProviderAndApiTeamIdIn(
                DataOrigin.BETS,
                Set.of("external-10", "external-30"));
        verify(teamExternalMappingRepository, never())
                .findByProviderAndApiTeamId(eq(DataOrigin.BETS), org.mockito.ArgumentMatchers.anyString());
    }

    private LeagueSeasonStandingsDataDocument.GroupMapping groupMapping(
            String groupKey,
            String displayName,
            Integer displayOrder) {

        return LeagueSeasonStandingsDataDocument.GroupMapping.builder()
                .groupKey(groupKey)
                .displayName(displayName)
                .displayOrder(displayOrder)
                .visibleInMatchList(true)
                .build();
    }
}
