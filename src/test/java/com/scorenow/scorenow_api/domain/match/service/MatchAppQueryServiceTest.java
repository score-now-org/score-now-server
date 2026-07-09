package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchAppQueryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchDetailRepository matchDetailRepository;

    @Spy
    private MatchDisplayTextResolver matchDisplayTextResolver = new MatchDisplayTextResolver();

    @InjectMocks
    private MatchAppQueryService matchAppQueryService;

    @Test
    void 진행중_경기는_MatchDetailDocument의_진행시간과_중계멘트를_조합한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(1L, MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        MatchDetailDocument detail = MatchDetailDocument.builder()
                .id(match.getId())
                .homeScore(1)
                .awayScore(0)
                .currentCommentary("선제골 이후 홈팀이 흐름을 잡습니다.")
                .matchClock(MatchDetailDocument.MatchClock.builder()
                        .elapsedMinutes(27)
                        .elapsedSeconds(14)
                        .period(MatchPeriod.FIRST_HALF)
                        .running(true)
                        .providerUpdatedAt(Instant.parse("2026-06-02T11:27:14Z"))
                        .build())
                .build();

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse leagueGroup = result.get(0);
        assertThat(leagueGroup.getLeagueId()).isEqualTo(2L);
        assertThat(leagueGroup.getLeagueName()).isEqualTo("Premier League");

        MatchAppResponse.MatchItemResponse response = leagueGroup.getMatches().get(0);
        assertThat(response.getCurrentCommentary()).isEqualTo("선제골 이후 홈팀이 흐름을 잡습니다.");
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(0);
        assertThat(response.getTimeInfo().getDisplayText()).isEqualTo("전반 27");
        assertThat(response.getTimeInfo().getElapsedMinutes()).isEqualTo(27);
        assertThat(response.getTimeInfo().getPeriodCode()).isEqualTo("FIRST_HALF");
    }

    @Test
    void 종료된_경기는_점수를_기반으로_승패정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(2L, MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 3);

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getTimeInfo().getDisplayText()).isEqualTo("홈팀 패");
        assertThat(response.getTimeInfo().getResult()).isEqualTo(MatchResult.AWAY_WIN.name());
        assertThat(response.getTimeInfo().getWinnerTeamId()).isEqualTo(20L);
        assertThat(response.getTimeInfo().getWinnerTeamName()).isEqualTo("Away");
    }

    @Test
    void 승부차기_점수가_있는_종료_경기는_승부차기_점수로_승패정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(
                5L,
                MatchStatus.ENDED,
                LocalDateTime.of(2026, 6, 2, 18, 0),
                1,
                1,
                4,
                3);

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(1);
        assertThat(response.getHomeShootOutScore()).isEqualTo(4);
        assertThat(response.getAwayShootOutScore()).isEqualTo(3);
        assertThat(response.getTimeInfo().getDisplayText()).isEqualTo("홈팀 승");
        assertThat(response.getTimeInfo().getResult()).isEqualTo(MatchResult.HOME_WIN.name());
        assertThat(response.getTimeInfo().getWinnerTeamId()).isEqualTo(10L);
        assertThat(response.getTimeInfo().getWinnerTeamName()).isEqualTo("Home");
    }

    @Test
    void 예정_경기는_경기_시작_시간을_HH_mm_형식으로_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(4L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 9, 5), 0, 0);

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getTimeInfo().getDisplayText()).isEqualTo("09:05");
        assertThat(response.getTimeInfo().getStartAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 9, 5));
    }

    @Test
    void 경기_목록을_리그별로_그룹핑하고_각_리그_하위의_조회_우선순위를_유지한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match eplInPlay = createMatch(1L, 2L, "Premier League", MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        Match laLigaScheduled = createMatch(2L, 3L, "La Liga", MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 21, 0), 0, 0);
        Match eplEnded = createMatch(3L, 2L, "Premier League", MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 1);

        givenAppMatches(date, 1L, null, eplInPlay, laLigaScheduled, eplEnded);
        given(matchDetailRepository.findAllById(List.of(1L, 2L, 3L))).willReturn(List.of());

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLeagueId()).isEqualTo(2L);
        assertThat(result.get(0).getLeagueName()).isEqualTo("Premier League");
        assertThat(result.get(0).getMatches()).extracting(MatchAppResponse.MatchItemResponse::getId)
                .containsExactly(1L, 3L);

        assertThat(result.get(1).getLeagueId()).isEqualTo(3L);
        assertThat(result.get(1).getLeagueName()).isEqualTo("La Liga");
        assertThat(result.get(1).getMatches()).extracting(MatchAppResponse.MatchItemResponse::getId)
                .containsExactly(2L);
    }

    private void givenAppMatches(LocalDate date, Long sportId, Long leagueId, Match... matches) {
        given(matchRepository.findAppMatches(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                sportId,
                leagueId,
                MatchAppStatusGroup.allStatuses(),
                MatchAppStatusGroup.IN_PLAY.getStatuses(),
                MatchAppStatusGroup.SCHEDULED.getStatuses(),
                MatchAppStatusGroup.ENDED.getStatuses()
        )).willReturn(List.of(matches));
    }

    private Match createMatch(Long id, MatchStatus status, LocalDateTime startAt, Integer homeScore, Integer awayScore) {
        return createMatch(id, 2L, "Premier League", status, startAt, homeScore, awayScore);
    }

    private Match createMatch(Long id, Long leagueId, String leagueName, MatchStatus status, LocalDateTime startAt, Integer homeScore, Integer awayScore) {
        return createMatch(id, leagueId, leagueName, status, startAt, homeScore, awayScore, null, null);
    }

    private Match createMatch(
            Long id,
            MatchStatus status,
            LocalDateTime startAt,
            Integer homeScore,
            Integer awayScore,
            Integer homeShootOutScore,
            Integer awayShootOutScore) {

        return createMatch(id, 2L, "Premier League", status, startAt, homeScore, awayScore, homeShootOutScore, awayShootOutScore);
    }

    private Match createMatch(
            Long id,
            Long leagueId,
            String leagueName,
            MatchStatus status,
            LocalDateTime startAt,
            Integer homeScore,
            Integer awayScore,
            Integer homeShootOutScore,
            Integer awayShootOutScore) {

        return Match.builder()
                .id(id)
                .sportId(1L)
                .leagueId(leagueId)
                .league(League.builder()
                        .id(leagueId)
                        .sName(leagueName)
                        .eName(leagueName)
                        .build())
                .homeId(10L)
                .homeTeam(Team.builder()
                        .id(10L)
                        .sName("Home")
                        .imageUrl("home.png")
                        .build())
                .awayId(20L)
                .awayTeam(Team.builder()
                        .id(20L)
                        .sName("Away")
                        .imageUrl("away.png")
                        .build())
                .startAt(startAt)
                .statusCode(status)
                .homeScore(homeScore)
                .homeShootOutScore(homeShootOutScore)
                .awayScore(awayScore)
                .awayShootOutScore(awayShootOutScore)
                .isActive(true)
                .teamDisplayOrder(TeamDisplayOrder.HOME_AWAY)
                .build();
    }
}
