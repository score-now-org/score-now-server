package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private MatchAppQueryService matchAppQueryService;

    @Test
    void 진행중_경기는_MatchDetailDocument의_진행시간과_중계멘트를_조합한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(1L, MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);
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

        givenAppMatches(date, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse response = result.get(0);
        assertThat(response.getCurrentCommentary()).isEqualTo("선제골 이후 홈팀이 흐름을 잡습니다.");
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(0);
        assertThat(response.getTimeInfo().getType()).isEqualTo("IN_PLAY");
        assertThat(response.getTimeInfo().getElapsedMinutes()).isEqualTo(27);
        assertThat(response.getTimeInfo().getPeriodCode()).isEqualTo("FIRST_HALF");
    }

    @Test
    void 종료된_경기는_점수를_기반으로_승패정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(2L, MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 3);

        givenAppMatches(date, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse response = result.get(0);
        assertThat(response.getTimeInfo().getType()).isEqualTo(MatchStatus.ENDED.name());
        assertThat(response.getTimeInfo().getResult()).isEqualTo(MatchResult.AWAY_WIN.name());
        assertThat(response.getTimeInfo().getWinnerTeamId()).isEqualTo(20L);
        assertThat(response.getTimeInfo().getWinnerTeamName()).isEqualTo("Away");
    }

    private void givenAppMatches(LocalDate date, Match match) {
        given(matchRepository.findAppMatches(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                1L,
                2L,
                List.of(MatchStatus.IN_PLAY, MatchStatus.NOT_STARTED, MatchStatus.ENDED),
                MatchStatus.IN_PLAY,
                MatchStatus.NOT_STARTED,
                MatchStatus.ENDED
        )).willReturn(List.of(match));
    }

    private Match createMatch(Long id, MatchStatus status, LocalDateTime startAt, Integer homeScore, Integer awayScore) {
        return Match.builder()
                .id(id)
                .sportId(1L)
                .leagueId(2L)
                .league(League.builder()
                        .id(2L)
                        .sName("EPL")
                        .eName("Premier League")
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
                .awayScore(awayScore)
                .isActive(true)
                .teamDisplayOrder(TeamDisplayOrder.HOME_AWAY)
                .build();
    }
}
