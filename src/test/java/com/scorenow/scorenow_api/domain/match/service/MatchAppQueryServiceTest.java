package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.service.standings.TeamStandingLookupService;
import com.scorenow.scorenow_api.domain.league.service.standings.model.LeagueTeamKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppItemResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppLeagueGroupResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.EndedStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatch;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.FeaturedMatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.appresponse.MatchAppSportResponseMapperRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.odds.dto.response.MatchOddsCategoryResponse;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.service.MatchOddsAppQueryService;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.TemporaryStadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MatchAppQueryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchDetailRepository matchDetailRepository;

    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;

    @Mock
    private MatchAppSportResponseMapperRegistry matchAppSportResponseMapperRegistry;

    @Mock
    private TeamStandingLookupService teamStandingLookupService;

    @Mock
    private FeaturedMatchRepository featuredMatchRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private MatchOddsAppQueryService matchOddsAppQueryService;

    @InjectMocks
    private MatchAppQueryService matchAppQueryService;

    @Test
    void 야구_경기_상세는_이닝_정보와_활성화된_상세중계_정보를_내려준다() {
        Match match = createBaseballMatch(100L);
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.FIRST_INNING_BOTTOM)
                .inningScores(BaseballInningScores.of(Map.of(
                        BaseballHalfInning.FIRST_INNING_TOP, 1,
                        BaseballHalfInning.FIRST_INNING_BOTTOM, 2
                )))
                .detailedBroadcastEnabled(true)
                .pitcher("홈 투수")
                .batter("원정 타자")
                .baseStates(BaseballBaseStates.of(Map.of(
                        BaseballBase.FIRST_BASE,
                        new BaseballBaseStates.BaseballBaseState(true, "1루 주자")
                )))
                .count(new BaseballCount(2, 1, 1))
                .homeStats(new BaseballStats(4, 0, 2))
                .awayStats(new BaseballStats(3, 1, 1))
                .build();
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportCode.BASEBALL)
                .sportDetail(baseballDetail)
                .build();
        MatchStatusDisplayResponse statusDisplay = statusDisplay("1회말");
        BaseballAppMatchDetailResponse sportDetailResponse = BaseballAppMatchDetailResponse.from(baseballDetail);
        MatchOddsCategoryResponse odds = MatchOddsCategoryResponse.of(OddsCategory.OVERSEAS, List.of());

        given(matchRepository.findById(match.getId())).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(match.getId())).willReturn(Optional.of(matchDetail));
        given(matchStatusDisplayResolver.resolve(match, matchDetail)).willReturn(statusDisplay);
        given(matchAppSportResponseMapperRegistry.mapDetail(matchDetail)).willReturn(sportDetailResponse);
        given(matchOddsAppQueryService.getOverseasWinDrawLossOddsByMatchIds(Set.of(match.getId())))
                .willReturn(Map.of(match.getId(), odds));
        given(teamStandingLookupService.getTeamStandings(
                Map.of(match.getLeagueId(), Set.of(match.getHomeId(), match.getAwayId())),
                match.getStartAt().toLocalDate()
        )).willReturn(Map.of());

        MatchAppDetailResponse response = matchAppQueryService.getMatch(match.getId());

        assertThat(response.sportCode()).isEqualTo(SportCode.BASEBALL);
        assertThat(response.homeScore()).isEqualTo(2);
        assertThat(response.awayScore()).isEqualTo(1);
        assertThat(response.statusDisplay()).isSameAs(statusDisplay);
        assertThat(response.sportDetail()).isSameAs(sportDetailResponse);
        assertThat(response.odds()).containsExactly(odds);

        BaseballAppMatchDetailResponse detail = (BaseballAppMatchDetailResponse) response.sportDetail();
        assertThat(detail.currentInning().code()).isEqualTo("FIRST_INNING_BOTTOM");
        assertThat(detail.inningScores().get(0).homeScore()).isEqualTo(2);
        assertThat(detail.inningScores().get(0).awayScore()).isEqualTo(1);
        assertThat(detail.detailedBroadcastEnabled()).isTrue();
        assertThat(detail.detailed().pitcher()).isEqualTo("홈 투수");
        assertThat(detail.detailed().batter()).isEqualTo("원정 타자");
        assertThat(detail.detailed().baseStates()).extracting(
                        BaseballAppMatchDetailResponse.BaseStateResponse::runnerOnBase)
                .containsExactly(true, false, false);
        assertThat(detail.detailed().count().balls()).isEqualTo(2);
        assertThat(detail.detailed().homeStats().hits()).isEqualTo(4);
        assertThat(detail.detailed().awayStats().errors()).isEqualTo(1);
    }

    @Test
    void 야구_상세중계가_비활성화되어도_이닝정보는_내려주고_detailed는_제외한다() {
        BaseballDetail baseballDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.SECOND_INNING_TOP)
                .inningScores(BaseballInningScores.initialize())
                .detailedBroadcastEnabled(false)
                .pitcher("노출하지 않을 투수")
                .build();

        BaseballAppMatchDetailResponse response = BaseballAppMatchDetailResponse.from(baseballDetail);

        assertThat(response.currentInning().code()).isEqualTo("SECOND_INNING_TOP");
        assertThat(response.inningScores()).hasSize(20);
        assertThat(response.detailedBroadcastEnabled()).isFalse();
        assertThat(response.detailed()).isNull();
    }

    @Test
    void 전체_경기_목록에서도_야구_종목코드와_라이브_요약을_내려준다() {
        LocalDate date = LocalDate.of(2026, 10, 14);
        Match match = createBaseballMatch(101L);
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportCode.BASEBALL)
                .sportDetail(BaseballDetail.builder()
                        .detailedBroadcastEnabled(true)
                        .build())
                .build();
        BaseballAppLiveSummaryResponse liveSummary = new BaseballAppLiveSummaryResponse(
                true,
                List.of(
                        new BaseballAppLiveSummaryResponse.BaseStateResponse(1, true),
                        new BaseballAppLiveSummaryResponse.BaseStateResponse(2, false),
                        new BaseballAppLiveSummaryResponse.BaseStateResponse(3, false)
                ),
                1
        );
        MatchOddsCategoryResponse odds = MatchOddsCategoryResponse.of(OddsCategory.OVERSEAS, List.of());

        givenAppMatches(date, null, null, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(matchDetail));
        given(matchStatusDisplayResolver.resolve(match, matchDetail)).willReturn(statusDisplay("1회말"));
        given(matchAppSportResponseMapperRegistry.mapLiveSummary(match, matchDetail)).willReturn(liveSummary);
        given(matchOddsAppQueryService.getOverseasWinDrawLossOddsByMatchIds(Set.of(match.getId())))
                .willReturn(Map.of(match.getId(), odds));

        MatchAppResponse response = matchAppQueryService.getMatchesWithFeatured(date, null, null);

        MatchAppLeagueGroupResponse leagueGroup = response.getLeagueMatches().get(0);
        MatchAppItemResponse matchItem = leagueGroup.getMatches().get(0);

        assertThat(leagueGroup.getSportCode()).isEqualTo(SportCode.BASEBALL);
        assertThat(matchItem.getSportCode()).isEqualTo(SportCode.BASEBALL);
        assertThat(matchItem.getSportLiveSummary()).isSameAs(liveSummary);
        assertThat(matchItem.getOdds()).containsExactly(odds);
    }

    @Test
    void 진행중_경기는_MatchDetailDocument의_중계멘트와_statusDisplay_정보를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(1L, MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("전반 27");
        MatchDetailDocument detail = MatchDetailDocument.builder()
                .id(match.getId())
                .type(SportCode.FOOTBALL)
                .currentCommentary("선제골 이후 홈팀이 흐름을 잡습니다.")
                .currentCommentaryHighlighted(true)
                .build();

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));
        given(matchStatusDisplayResolver.resolve(match, detail)).willReturn(statusDisplayResponse);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result).hasSize(1);
        MatchAppLeagueGroupResponse leagueGroup = result.get(0);
        assertThat(leagueGroup.getLeagueId()).isEqualTo(2L);
        assertThat(leagueGroup.getLeagueName()).isEqualTo("Premier League");

        MatchAppItemResponse response = leagueGroup.getMatches().get(0);
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

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplayResponse);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result).hasSize(1);
        MatchAppItemResponse response = result.get(0).getMatches().get(0);
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
                .type(SportCode.FOOTBALL)
                .sportDetail(FootballDetail.builder()
                        .shootOutScore(FootballShootOutScore.builder()
                                .homeScore(4)
                                .awayScore(3)
                                .build())
                        .build())
                .build();

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of(detail));
        given(matchStatusDisplayResolver.resolve(match, detail)).willReturn(statusDisplayResponse);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result).hasSize(1);
        MatchAppItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getHomeScore()).isEqualTo(1);
        assertThat(response.getAwayScore()).isEqualTo(1);
        assertThat(response.getStatusDisplay()).isSameAs(statusDisplayResponse);
    }

    @Test
    void 예정_경기는_statusDisplay_resolver가_만든_시작시간_문구를_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(4L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 9, 5), 0, 0);
        MatchStatusDisplayResponse statusDisplayResponse = statusDisplay("09:05");

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplayResponse);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result).hasSize(1);
        MatchAppItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getStatusDisplay()).isSameAs(statusDisplayResponse);
        assertThat(response.getStatusDisplay().getDisplayText()).isEqualTo("09:05");
    }

    @Test
    void 경기_목록을_리그별로_그룹핑하고_각_리그_하위의_조회_우선순위를_유지한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match eplInPlay = createMatch(1L, 2L, "Premier League", MatchStatus.IN_PLAY, LocalDateTime.of(2026, 6, 2, 20, 0), 1, 0);
        Match laLigaScheduled = createMatch(2L, 3L, "La Liga", MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 21, 0), 0, 0);
        Match eplEnded = createMatch(3L, 2L, "Premier League", MatchStatus.ENDED, LocalDateTime.of(2026, 6, 2, 18, 0), 2, 1);

        givenAppMatches(date, SportCode.FOOTBALL, null, eplInPlay, laLigaScheduled, eplEnded);
        given(matchDetailRepository.findAllById(List.of(1L, 2L, 3L))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(eplInPlay, null)).willReturn(statusDisplay("전반 1"));
        given(matchStatusDisplayResolver.resolve(laLigaScheduled, null)).willReturn(statusDisplay("21:00"));
        given(matchStatusDisplayResolver.resolve(eplEnded, null)).willReturn(statusDisplay("홈팀 승"));

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, null).getLeagueMatches();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLeagueId()).isEqualTo(2L);
        assertThat(result.get(0).getLeagueName()).isEqualTo("Premier League");
        assertThat(result.get(0).getMatches()).extracting(MatchAppItemResponse::getId)
                .containsExactly(1L, 3L);

        assertThat(result.get(1).getLeagueId()).isEqualTo(3L);
        assertThat(result.get(1).getLeagueName()).isEqualTo("La Liga");
        assertThat(result.get(1).getMatches()).extracting(MatchAppItemResponse::getId)
                .containsExactly(2L);
        verify(matchOddsAppQueryService).getOverseasWinDrawLossOddsByMatchIds(Set.of(1L, 2L, 3L));
    }

    @Test
    void 경기_팀의_순위를_리그와_팀별로_조회하여_API_응답으로_변환한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(1L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);

        givenAppMatches(
                date,
                SportCode.FOOTBALL,
                2L,
                Map.of(
                        new LeagueTeamKey(2L, 10L),
                        List.of(new TeamStandingSummary("통합", 1, 3))),
                match);
        given(matchDetailRepository.findAllById(List.of(match.getId()))).willReturn(List.of());
        given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplay("20:00"));

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        MatchAppItemResponse response = result.get(0).getMatches().get(0);
        assertThat(response.getHomeTeam().getStandings()).hasSize(1);
        assertThat(response.getHomeTeam().getStandings().get(0).getGroupName()).isEqualTo("통합");
        assertThat(response.getHomeTeam().getStandings().get(0).getRank()).isEqualTo(3);
        assertThat(response.getAwayTeam().getStandings()).isEmpty();
    }

    @Test
    void 등록_경기장이_매핑되어_있으면_경기장명과_도시명을_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatchBuilder(
                51L,
                2L,
                "Premier League",
                MatchStatus.NOT_STARTED,
                LocalDateTime.of(2026, 6, 2, 20, 0),
                0,
                0)
                .stadiumId(101L)
                .build();
        Stadium stadium = stadium(101L, "서울월드컵경기장", "서울", true);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);
        given(stadiumRepository.findAllByIds(Set.of(101L))).willReturn(List.of(stadium));

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        MatchAppItemResponse.StadiumResponse response = result.get(0).getMatches().get(0).getStadium();
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("서울월드컵경기장");
        assertThat(response.getCity()).isEqualTo("서울");
    }

    @Test
    void 임시_경기장과_등록_경기장이_모두_있으면_임시_경기장을_우선하여_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatchBuilder(
                52L,
                2L,
                "Premier League",
                MatchStatus.NOT_STARTED,
                LocalDateTime.of(2026, 6, 2, 20, 0),
                0,
                0)
                .stadiumId(101L)
                .temporaryStadium(new TemporaryStadium("임시 축구장", "부산"))
                .build();

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        MatchAppItemResponse.StadiumResponse response = result.get(0).getMatches().get(0).getStadium();
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("임시 축구장");
        assertThat(response.getCity()).isEqualTo("부산");
        verifyNoInteractions(stadiumRepository);
    }

    @Test
    void 경기장이_매핑되어_있지_않으면_stadium을_null로_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(53L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result.get(0).getMatches().get(0).getStadium()).isNull();
        verifyNoInteractions(stadiumRepository);
    }

    @Test
    void 매핑된_경기장_ID에_해당하는_경기장이_없어도_목록_조회에_성공하고_stadium을_null로_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatchBuilder(
                54L,
                2L,
                "Premier League",
                MatchStatus.NOT_STARTED,
                LocalDateTime.of(2026, 6, 2, 20, 0),
                0,
                0)
                .stadiumId(999L)
                .build();

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);
        given(stadiumRepository.findAllByIds(Set.of(999L))).willReturn(List.of());

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatches().get(0).getStadium()).isNull();
    }

    @Test
    void 비활성_경기장도_기존_경기에_매핑되어_있으면_경기장명과_도시명을_내려준다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatchBuilder(
                55L,
                2L,
                "Premier League",
                MatchStatus.NOT_STARTED,
                LocalDateTime.of(2026, 6, 2, 20, 0),
                0,
                0)
                .stadiumId(102L)
                .build();
        Stadium inactiveStadium = stadium(102L, "폐쇄된 경기장", "인천", false);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);
        given(stadiumRepository.findAllByIds(Set.of(102L))).willReturn(List.of(inactiveStadium));

        List<MatchAppLeagueGroupResponse> result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L).getLeagueMatches();

        MatchAppItemResponse.StadiumResponse response = result.get(0).getMatches().get(0).getStadium();
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("폐쇄된 경기장");
        assertThat(response.getCity()).isEqualTo("인천");
    }

    @Test
    void v2는_상단고정과_핫매치를_유형별로_분리하여_반환한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match pinnedMatch = createMatch(11L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 19, 0), 0, 0);
        Match hotMatch = createMatch(12L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, pinnedMatch, hotMatch);
        givenMatchResponseData(pinnedMatch, hotMatch);
        given(featuredMatchRepository.findAllByDisplayDateAndMatchIds(date, Set.of(11L, 12L)))
                .willReturn(List.of(
                        featuredMatch(11L, date, FeaturedMatchType.PINNED, 1),
                        featuredMatch(12L, date, FeaturedMatchType.HOT_MATCH, 1)
                ));

        MatchAppResponse result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L);

        assertThat(result.getFeaturedMatches().getPinnedMatches())
                .extracting(featured -> featured.getMatch().getId())
                .containsExactly(11L);
        assertThat(result.getFeaturedMatches().getHotMatches())
                .extracting(featured -> featured.getMatch().getId())
                .containsExactly(12L);
    }

    @Test
    void v2는_Featured_저장소가_반환한_유형별_노출_순서를_유지한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match pinnedSecond = createMatch(21L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 19, 0), 0, 0);
        Match pinnedFirst = createMatch(22L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);
        Match hotSecond = createMatch(23L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 21, 0), 0, 0);
        Match hotFirst = createMatch(24L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 22, 0), 0, 0);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, pinnedSecond, pinnedFirst, hotSecond, hotFirst);
        givenMatchResponseData(pinnedSecond, pinnedFirst, hotSecond, hotFirst);
        given(featuredMatchRepository.findAllByDisplayDateAndMatchIds(date, Set.of(21L, 22L, 23L, 24L)))
                .willReturn(List.of(
                        featuredMatch(24L, date, FeaturedMatchType.HOT_MATCH, 1),
                        featuredMatch(23L, date, FeaturedMatchType.HOT_MATCH, 2),
                        featuredMatch(22L, date, FeaturedMatchType.PINNED, 1),
                        featuredMatch(21L, date, FeaturedMatchType.PINNED, 2)
                ));

        MatchAppResponse result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L);

        assertThat(result.getFeaturedMatches().getPinnedMatches())
                .extracting(featured -> featured.getMatch().getId())
                .containsExactly(22L, 21L);
        assertThat(result.getFeaturedMatches().getHotMatches())
                .extracting(featured -> featured.getMatch().getId())
                .containsExactly(24L, 23L);
    }

    @Test
    void v2의_Featured_경기는_기존_리그별_경기_목록에도_유지된다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match match = createMatch(31L, MatchStatus.NOT_STARTED, LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, match);
        givenMatchResponseData(match);
        given(featuredMatchRepository.findAllByDisplayDateAndMatchIds(date, Set.of(31L)))
                .willReturn(List.of(featuredMatch(31L, date, FeaturedMatchType.PINNED, 1)));

        MatchAppResponse result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L);

        MatchAppItemResponse leagueMatch = result.getLeagueMatches().get(0).getMatches().get(0);
        MatchAppItemResponse featuredMatch = result.getFeaturedMatches().getPinnedMatches().get(0).getMatch();

        assertThat(result.getLeagueMatches()).hasSize(1);
        assertThat(leagueMatch.getId()).isEqualTo(31L);
        assertThat(featuredMatch.getId()).isEqualTo(31L);
        assertThat(featuredMatch).isSameAs(leagueMatch);
    }

    @Test
    void v2는_기존_조회_조건으로_조회된_경기_ID만_Featured_조회에_사용한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        Match filteredMatch = createMatch(41L, 2L, "Premier League", MatchStatus.NOT_STARTED,
                LocalDateTime.of(2026, 6, 2, 20, 0), 0, 0);

        givenAppMatches(date, SportCode.FOOTBALL, 2L, filteredMatch);
        givenMatchResponseData(filteredMatch);
        given(featuredMatchRepository.findAllByDisplayDateAndMatchIds(date, Set.of(41L)))
                .willReturn(List.of(featuredMatch(41L, date, FeaturedMatchType.HOT_MATCH, 1)));

        MatchAppResponse result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L);

        verify(featuredMatchRepository).findAllByDisplayDateAndMatchIds(date, Set.of(41L));
        assertThat(result.getFeaturedMatches().getPinnedMatches()).isEmpty();
        assertThat(result.getFeaturedMatches().getHotMatches())
                .extracting(featured -> featured.getMatch().getId())
                .containsExactly(41L);
    }

    @Test
    void v2는_기존_경기_목록이_비어있으면_Featured_저장소를_호출하지_않고_빈_목록을_반환한다() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        given(matchRepository.findAppMatches(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                SportCode.FOOTBALL,
                2L,
                MatchAppStatusGroup.allStatuses(),
                MatchAppStatusGroup.IN_PLAY.getStatuses(),
                MatchAppStatusGroup.SCHEDULED.getStatuses(),
                MatchAppStatusGroup.ENDED.getStatuses()
        )).willReturn(List.of());

        MatchAppResponse result = matchAppQueryService.getMatchesWithFeatured(date, SportCode.FOOTBALL, 2L);

        verifyNoInteractions(featuredMatchRepository);
        assertThat(result.getLeagueMatches()).isEmpty();
        assertThat(result.getFeaturedMatches().getPinnedMatches()).isEmpty();
        assertThat(result.getFeaturedMatches().getHotMatches()).isEmpty();
    }

    private void givenAppMatches(LocalDate date, SportCode sportCode, Long leagueId, Match... matches) {
        givenAppMatches(date, sportCode, leagueId, Map.of(), matches);
    }

    private void givenAppMatches(
            LocalDate date,
            SportCode sportCode,
            Long leagueId,
            Map<LeagueTeamKey, List<TeamStandingSummary>> teamStandings,
            Match... matches) {

        given(matchRepository.findAppMatches(
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                sportCode,
                leagueId,
                MatchAppStatusGroup.allStatuses(),
                MatchAppStatusGroup.IN_PLAY.getStatuses(),
                MatchAppStatusGroup.SCHEDULED.getStatuses(),
                MatchAppStatusGroup.ENDED.getStatuses()
        )).willReturn(List.of(matches));

        Map<Long, Set<Long>> teamIdsByLeague = new java.util.LinkedHashMap<>();
        for (Match match : matches) {
            teamIdsByLeague.computeIfAbsent(match.getLeagueId(), ignored -> new java.util.HashSet<>())
                    .addAll(Set.of(match.getHomeId(), match.getAwayId()));
        }
        given(teamStandingLookupService.getTeamStandings(teamIdsByLeague, date)).willReturn(teamStandings);
    }

    private void givenMatchResponseData(Match... matches) {
        List<Long> matchIds = Arrays.stream(matches)
                .map(Match::getId)
                .toList();
        given(matchDetailRepository.findAllById(matchIds)).willReturn(List.of());

        for (Match match : matches) {
            given(matchStatusDisplayResolver.resolve(match, null)).willReturn(statusDisplay("경기 예정"));
        }
    }

    private FeaturedMatch featuredMatch(
            Long matchId,
            LocalDate displayDate,
            FeaturedMatchType type,
            Integer displayOrder
    ) {
        return new FeaturedMatch(matchId, displayDate, type, displayOrder);
    }

    private Match createMatch(Long id, MatchStatus status, LocalDateTime startAt, Integer homeScore, Integer awayScore) {
        return createMatch(id, 2L, "Premier League", status, startAt, homeScore, awayScore);
    }

    private Match createMatch(Long id, Long leagueId, String leagueName, MatchStatus status, LocalDateTime startAt, Integer homeScore, Integer awayScore) {
        return createMatchBuilder(id, leagueId, leagueName, status, startAt, homeScore, awayScore)
                .build();
    }

    private Match.MatchBuilder createMatchBuilder(
            Long id,
            Long leagueId,
            String leagueName,
            MatchStatus status,
            LocalDateTime startAt,
            Integer homeScore,
            Integer awayScore) {
        return Match.builder()
                .id(id)
                .sportId(1L)
                .sport(Sport.builder()
                        .id(1L)
                        .sportCode(SportCode.FOOTBALL)
                        .build())
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
                .teamDisplayOrder(TeamDisplayOrder.HOME_AWAY);
    }

    private Match createBaseballMatch(Long id) {
        return Match.builder()
                .id(id)
                .sportId(2L)
                .sport(Sport.builder()
                        .id(2L)
                        .sportCode(SportCode.BASEBALL)
                        .build())
                .leagueId(20L)
                .league(League.builder()
                        .id(20L)
                        .sName("KBO")
                        .eName("KBO")
                        .build())
                .homeId(100L)
                .homeTeam(Team.builder()
                        .id(100L)
                        .sName("홈팀")
                        .build())
                .awayId(200L)
                .awayTeam(Team.builder()
                        .id(200L)
                        .sName("원정팀")
                        .build())
                .startAt(LocalDateTime.of(2026, 10, 14, 18, 30))
                .statusCode(MatchStatus.IN_PLAY)
                .homeScore(2)
                .awayScore(1)
                .isActive(true)
                .teamDisplayOrder(TeamDisplayOrder.HOME_AWAY)
                .build();
    }

    private Stadium stadium(Long id, String name, String city, boolean isActive) {
        return Stadium.builder()
                .id(id)
                .sportId(1L)
                .name(name)
                .city(city)
                .isActive(isActive)
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
