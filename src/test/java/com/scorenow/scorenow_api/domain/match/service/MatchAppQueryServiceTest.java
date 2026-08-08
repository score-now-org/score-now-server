package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.EndedStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;

    @InjectMocks
    private MatchAppQueryService matchAppQueryService;

    @Test
    void 진행중_경기는_MatchDetailDocument의_중계멘트와_statusDisplay_정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(1L, MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("전반 27");
        MatchDetailDocument detail = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportDetailType.FOOTBALL)
                .currentCommentary("선제골 이후 홈팀이 흐름을 잡습니다.")
                .currentCommentaryHighlighted(true)
                .build();

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));
        given(matchStatusDisplayResolver.resolve(match, detail)).willReturn(statusDisplayResponse);

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse leagueGroup = result.get(0);
        assertThat(leagueGroup.getLeagueId()).isEqualTo(2L);
        assertThat(leagueGroup.getLeagueName()).isEqualTo("Premier League");

        MatchAppResponse.MatchItemResponse response = leagueGroup.getMatches().get(0);
        assertThat(response.getCurrentCommentary()).isEqualTo("선제골 이후 홈팀이 흐름을 잡습니다.");
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(0);
        assertThat(response.getStatusDisplay()).isSameAs(statusDisplayResponse);
        assertThat(response.getStatusDisplay().getDisplayText()).isEqualTo("전반 27");
    }

    @Test
    void 종료된_경기는_statusDisplay_resolver가_만든_승패정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(2L, MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 3);
        EndedStatusDisplayDetailResponse endedDetail = EndedStatusDisplayDetailResponse.builder()
                .result(MatchResult.AWAY_WIN.name())
                .winnerTeamId(20L)
                .winnerTeamName("Away")
                .build();
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("홈팀 패", endedDetail);

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplayResponse);

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getStatusDisplay().getDisplayText()).isEqualTo("홈팀 패");
        assertThat(response.getStatusDisplay().getDetail()).isSameAs(endedDetail);
    }

    @Test
    void 종료된_축구_경기는_상세_문서를_statusDisplay_resolver에_전달한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(5L, MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 1, 1);
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("홈팀 승", EndedStatusDisplayDetailResponse.builder()
                .result(MatchResult.HOME_WIN.name())
                .winnerTeamId(10L)
                .winnerTeamName("Home")
                .build());
        MatchDetailDocument detail = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportDetailType.FOOTBALL)
                .sportDetail(FootballDetail.builder()
                        .shootOutScore(FootballShootOutScore.builder()
                                .homeScore(4)
                                .awayScore(3)
                                .build())
                        .build())
                .build();

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));
        given(matchStatusDisplayResolver.resolve(match, detail)).willReturn(statusDisplayResponse);

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(1);
        assertThat(response.getStatusDisplay()).isSameAs(statusDisplayResponse);
    }

    @Test
    void 예정_경기는_statusDisplay_resolver가_만든_시작시간_문구를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(4L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 9, 5), 0, 0);
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("09:05");

        givenAppMatches(date, 1L, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplayResponse);

        List<MatchAppResponse> result = matchAppQueryService.getMatches(date, 1L, 2L);

        assertThat(result).hasSize(1);
        MatchAppResponse.MatchItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getStatusDisplay()).isSameAs(statusDisplayResponse);
        assertThat(response.getStatusDisplay().getDisplayText()).isEqualTo("09:05");
    }

    @Test
    void 경기_목록을_리그별로_그룹핑하고_각_리그_하위의_조회_우선순위를_유지한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match eplInPlay = createMatch(1L, 2L, "Premier League", MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        Match laLigaScheduled = createMatch(2L, 3L, "La Liga", MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 21, 0), 0, 0);
        Match eplEnded = createMatch(3L, 2L, "Premier League", MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 1);

        givenAppMatches(date, 1L, null, eplInPlay, laLigaScheduled, eplEnded);
        given(matchDetailRepository.findAllById(List.of(1L, 2L, 3L))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(eplInPlay, null)).willReturn(statusDisplay("전반 1"));
        given(matchStatusDisplayResolver.resolve(laLigaScheduled, null)).willReturn(statusDisplay("21:00"));
        given(matchStatusDisplayResolver.resolve(eplEnded, null)).willReturn(statusDisplay("홈팀 승"));

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
                .awayScore(awayScore)
                .isActive(true)
                .teamDisplayOrder(TeamDisplayOrder.HOME_AWAY)
                .build();
    }

    private MatchStatusDisplayResponse statusDisplay(String displayText) {
        return statusDisplay(displayText, null);
    }

    private MatchStatusDisplayResponse statusDisplay(String displayText, Object detail) {
        return MatchStatusDisplayResponse.builder()
                .displayText(displayText)
                .detail(detail)
                .build();
    }
}
