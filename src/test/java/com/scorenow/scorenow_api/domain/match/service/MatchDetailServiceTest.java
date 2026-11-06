package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncStadium;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchDetailServiceTest {

    @InjectMocks
    private MatchDetailService matchDetailService;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailRepository matchDetailRepository;
    @Mock
    private StadiumCacheService stadiumCacheService;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;
    @Mock
    private SportDetailEventProcessorRegistry eventProcessorRegistry;
    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;
    @Mock
    private TeamExternalLookupService teamExternalLookupService;

    @Test
    void 진행중_경기_상세_동기화시_MatchDetailDocument가_없으면_예외가_발생한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(1L)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.FOOTBALL,
                matchId,
                "1",
                null,
                null,
                0,
                0,
                MatchStatus.IN_PLAY,
                null,
                FootballDetail.empty()
        );

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchDetailService.updateInplayMatchDetail(snapshot))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MATCH_DETAIL_NOT_FOUND.getMessage());
    }

    @Test
    void 야구_View_동기화시_Bets_이닝_정보만_교체하고_관리자_상세_정보는_보존한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(9L)
                .homeId(1L)
                .awayId(2L)
                .homeScore(2)
                .awayScore(1)
                .statusCode(MatchStatus.IN_PLAY)
                .build();

        BaseballCount currentCount = new BaseballCount(2, 1, 1);
        BaseballBaseStates currentBaseStates = BaseballBaseStates.of(Map.of(
                BaseballBase.FIRST_BASE,
                new BaseballBaseStates.BaseballBaseState(true, "1루 주자")));
        BaseballStats currentHomeStats = new BaseballStats(5, 1, 2);
        BaseballStats currentAwayStats = new BaseballStats(4, 0, 1);
        BaseballDetail currentDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.SECOND_INNING_BOTTOM)
                .inningScores(inningScores(Map.of(
                        BaseballHalfInning.FIRST_INNING_TOP, 1,
                        BaseballHalfInning.FIRST_INNING_BOTTOM, 0)))
                .detailedBroadcastEnabled(true)
                .pitcher("현재 투수")
                .batter("현재 타자")
                .baseStates(currentBaseStates)
                .count(currentCount)
                .homeStats(currentHomeStats)
                .awayStats(currentAwayStats)
                .build();
        MatchDetailDocument currentDocument = baseballDocument(matchId, currentDetail);

        BaseballInningScores incomingScores = inningScores(Map.of(
                BaseballHalfInning.FIRST_INNING_TOP, 1,
                BaseballHalfInning.FIRST_INNING_BOTTOM, 0,
                BaseballHalfInning.SECOND_INNING_TOP, 0,
                BaseballHalfInning.SECOND_INNING_BOTTOM, 2));
        BaseballDetail incomingDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.SECOND_INNING_BOTTOM)
                .inningScores(incomingScores)
                .build();
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.BASEBALL,
                matchId,
                "3",
                "view-home",
                "view-away",
                2,
                1,
                MatchStatus.IN_PLAY,
                null,
                incomingDetail);

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.of(currentDocument));
        givenBaseballTeamMapping();

        matchDetailService.updateInplayMatchDetail(snapshot);

        ArgumentCaptor<MatchDetailDocument> newDocumentCaptor =
                ArgumentCaptor.forClass(MatchDetailDocument.class);
        then(eventProcessorRegistry).should().process(eq(currentDocument), newDocumentCaptor.capture());
        MatchDetailDocument newDocument = newDocumentCaptor.getValue();
        BaseballDetail mergedDetail = (BaseballDetail) newDocument.getSportDetail();

        assertThat(mergedDetail.getCurrentHalfInning())
                .isEqualTo(BaseballHalfInning.SECOND_INNING_BOTTOM);
        assertThat(mergedDetail.getInningScores().getScoresByHalfInning())
                .isEqualTo(incomingScores.getScoresByHalfInning());
        assertThat(mergedDetail.isDetailedBroadcastEnabled()).isTrue();
        assertThat(mergedDetail.getPitcher()).isEqualTo("현재 투수");
        assertThat(mergedDetail.getBatter()).isEqualTo("현재 타자");
        assertThat(mergedDetail.getBaseStates()).isSameAs(currentBaseStates);
        assertThat(mergedDetail.getCount()).isSameAs(currentCount);
        assertThat(mergedDetail.getHomeStats()).isSameAs(currentHomeStats);
        assertThat(mergedDetail.getAwayStats()).isSameAs(currentAwayStats);

        then(matchDetailRepository).should().upsertMatchDetail(newDocument);
        then(eventPublisher).shouldHaveNoInteractions();
        then(stadiumCacheService).shouldHaveNoInteractions();
        then(matchStatusDisplayResolver).shouldHaveNoInteractions();
    }

    @Test
    void 야구_View_동기화시_점수_상태_경기장을_함께_갱신한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(9L)
                .homeId(1L)
                .awayId(2L)
                .homeScore(0)
                .awayScore(0)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        MatchDetailDocument currentDocument = baseballDocument(matchId, BaseballDetail.empty());
        MatchDetailSyncStadium stadium = new MatchDetailSyncStadium(
                "stadium-1",
                "Baseball Stadium",
                "Seoul");
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.BASEBALL,
                matchId,
                "3",
                "view-home",
                "view-away",
                2,
                3,
                MatchStatus.ENDED,
                stadium,
                BaseballDetail.empty());
        MatchStatusDisplayResponse statusDisplay = MatchStatusDisplayResponse.builder()
                .displayText("경기 종료")
                .build();

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.of(currentDocument));
        givenBaseballTeamMapping();
        given(stadiumCacheService.getOrCreateStadiumId(
                DataOrigin.BETS,
                "3",
                "stadium-1",
                "Baseball Stadium",
                "Seoul"))
                .willReturn(77L);
        given(matchStatusDisplayResolver.resolve(eq(match), any(MatchDetailDocument.class)))
                .willReturn(statusDisplay);

        matchDetailService.updateInplayMatchDetail(snapshot);

        assertThat(match.getHomeScore()).isEqualTo(2);
        assertThat(match.getAwayScore()).isEqualTo(3);
        assertThat(match.getStatusCode()).isEqualTo(MatchStatus.ENDED);
        assertThat(match.getStadiumId()).isEqualTo(77L);
        then(eventPublisher).should().publishScoreChanged(matchId, 2, 3);
        then(eventPublisher).should()
                .publishMatchStatusChanged(matchId, MatchStatus.ENDED, "경기 종료");
        then(eventProcessorRegistry).should().process(eq(currentDocument), any(MatchDetailDocument.class));
        then(matchDetailRepository).should().upsertMatchDetail(any(MatchDetailDocument.class));
    }

    @Test
    void 야구_View의_팀이_정확히_반대이면_팀만_교정하고_실시간_이벤트는_발행하지_않는다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(9L)
                .homeId(2L)
                .awayId(1L)
                .homeScore(3)
                .awayScore(2)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        MatchDetailDocument currentDocument = baseballDocument(matchId, BaseballDetail.empty());
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.BASEBALL,
                matchId,
                "3",
                "view-home",
                "view-away",
                3,
                2,
                MatchStatus.IN_PLAY,
                null,
                BaseballDetail.empty());

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.of(currentDocument));
        givenBaseballTeamMapping();

        matchDetailService.updateInplayMatchDetail(snapshot);

        assertThat(match.getHomeId()).isEqualTo(1L);
        assertThat(match.getAwayId()).isEqualTo(2L);
        assertThat(match.getHomeScore()).isEqualTo(3);
        assertThat(match.getAwayScore()).isEqualTo(2);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 야구_View의_팀이_기존_팀과_다르면_교정하지_않고_예외가_발생한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(9L)
                .homeId(3L)
                .awayId(4L)
                .homeScore(0)
                .awayScore(0)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.BASEBALL,
                matchId,
                "3",
                "view-home",
                "view-away",
                3,
                2,
                MatchStatus.IN_PLAY,
                null,
                BaseballDetail.empty());

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        givenBaseballTeamMapping();

        assertThatThrownBy(() -> matchDetailService.updateInplayMatchDetail(snapshot))
                .isInstanceOf(BusinessException.class)
                .hasMessage("야구 View API 경기 팀 정보가 기존 Match와 일치하지 않습니다.");

        assertThat(match.getHomeId()).isEqualTo(3L);
        assertThat(match.getAwayId()).isEqualTo(4L);
        assertThat(match.getHomeScore()).isZero();
        assertThat(match.getAwayScore()).isZero();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 야구_View에서_연장_이닝이_사라져도_기존_10회부터_12회까지_보존한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .apiMatchId("api-1000")
                .sportId(9L)
                .homeId(1L)
                .awayId(2L)
                .homeScore(1)
                .awayScore(1)
                .statusCode(MatchStatus.IN_PLAY)
                .build();

        EnumMap<BaseballHalfInning, Integer> currentValues = new EnumMap<>(BaseballHalfInning.class);
        for (int inning = 1; inning <= 12; inning++) {
            currentValues.put(BaseballHalfInning.getTopInning(inning), 0);
            currentValues.put(BaseballHalfInning.getBottomInning(inning), 0);
        }
        BaseballDetail currentDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.TWELFTH_INNING_BOTTOM)
                .inningScores(BaseballInningScores.of(currentValues))
                .build();

        EnumMap<BaseballHalfInning, Integer> incomingValues = new EnumMap<>(BaseballHalfInning.class);
        for (int inning = 1; inning <= 9; inning++) {
            incomingValues.put(BaseballHalfInning.getTopInning(inning), 0);
            incomingValues.put(BaseballHalfInning.getBottomInning(inning), 0);
        }
        BaseballDetail incomingDetail = BaseballDetail.builder()
                .currentHalfInning(BaseballHalfInning.NINTH_INNING_BOTTOM)
                .inningScores(BaseballInningScores.of(incomingValues))
                .build();
        MatchDetailDocument currentDocument = baseballDocument(matchId, currentDetail);
        MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                DataOrigin.BETS,
                SportCode.BASEBALL,
                matchId,
                "3",
                "view-home",
                "view-away",
                1,
                1,
                MatchStatus.IN_PLAY,
                null,
                incomingDetail);

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.of(currentDocument));
        givenBaseballTeamMapping();

        matchDetailService.updateInplayMatchDetail(snapshot);

        ArgumentCaptor<MatchDetailDocument> documentCaptor =
                ArgumentCaptor.forClass(MatchDetailDocument.class);
        then(matchDetailRepository).should().upsertMatchDetail(documentCaptor.capture());
        BaseballDetail mergedDetail = (BaseballDetail) documentCaptor.getValue().getSportDetail();

        assertThat(mergedDetail.getInningScores()
                .getHalfInningScore(BaseballHalfInning.TENTH_INNING_TOP)).isZero();
        assertThat(mergedDetail.getInningScores()
                .getHalfInningScore(BaseballHalfInning.TWELFTH_INNING_BOTTOM)).isZero();
        assertThat(mergedDetail.getCurrentHalfInning())
                .isEqualTo(BaseballHalfInning.TWELFTH_INNING_BOTTOM);
    }

    private MatchDetailDocument baseballDocument(Long matchId, BaseballDetail baseballDetail) {
        return MatchDetailDocument.builder()
                .id(matchId)
                .type(SportCode.BASEBALL)
                .sportDetail(baseballDetail)
                .build();
    }

    private BaseballInningScores inningScores(Map<BaseballHalfInning, Integer> values) {
        EnumMap<BaseballHalfInning, Integer> scores = new EnumMap<>(BaseballHalfInning.class);
        scores.putAll(values);
        return BaseballInningScores.of(scores);
    }

    private void givenBaseballTeamMapping() {
        given(teamExternalLookupService.findTeamsByExternalInfo(
                DataOrigin.BETS,
                List.of("view-home", "view-away")))
                .willReturn(Map.of(
                        "view-home", Team.builder().id(1L).build(),
                        "view-away", Team.builder().id(2L).build()
                ));
    }

}
