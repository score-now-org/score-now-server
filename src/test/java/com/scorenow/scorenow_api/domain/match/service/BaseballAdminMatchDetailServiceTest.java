package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.BaseballMatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.service.SportCodeResolver;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BaseballAdminMatchDetailServiceTest {

    private static final Long MATCH_ID = 1L;
    private static final Long SPORT_ID = 9L;

    @InjectMocks
    private BaseballAdminMatchDetailService service;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailRepository matchDetailRepository;
    @Mock
    private BaseballMatchDetailRepository baseballMatchDetailRepository;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;
    @Mock
    private SportDetailEventProcessorRegistry eventProcessorRegistry;
    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;
    @Mock
    private MatchScheduledStartAtUpdater matchScheduledStartAtUpdater;
    @Mock
    private SportCodeResolver sportCodeResolver;

    @Test
    void 야구_경기가_아니면_이닝_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateCurrentHalfInning(MATCH_ID, BaseballHalfInning.FIRST_INNING_TOP))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_이닝_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCurrentHalfInning(MATCH_ID, BaseballHalfInning.FIRST_INNING_TOP))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_스탯_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateBaseballStats(MATCH_ID, new BaseballStats(0, 0, 0), new BaseballStats(0, 0, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_스탯_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBaseballStats(MATCH_ID, new BaseballStats(0, 0, 0), new BaseballStats(0, 0, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_카운트_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateBaseballCount(MATCH_ID, BaseballCount.empty()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_카운트_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBaseballCount(MATCH_ID, BaseballCount.empty()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_베이스_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateBaseStates(MATCH_ID, BaseballBaseStates.initialize()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_베이스_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBaseStates(MATCH_ID, BaseballBaseStates.initialize()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_투수_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updatePitcher(MATCH_ID, "투수"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_투수_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePitcher(MATCH_ID, "투수"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_타자_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateBatter(MATCH_ID, "타자"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_타자_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBatter(MATCH_ID, "타자"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 야구_경기가_아니면_상세_중계_설정_수정을_거절한다() {
        givenMatchSport(SportCode.FOOTBALL);

        assertThatThrownBy(() -> service.updateDetailedBroadcastEnabled(MATCH_ID, true))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PARAMETER));

        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 경기가_없으면_상세_중계_설정_수정을_거절한다() {
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDetailedBroadcastEnabled(MATCH_ID, true))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MATCH_NOT_FOUND));

        then(sportCodeResolver).shouldHaveNoInteractions();
        then(matchDetailRepository).shouldHaveNoInteractions();
        then(baseballMatchDetailRepository).shouldHaveNoInteractions();
    }

    @Test
    void 상세가_없어도_이닝과_초기화된_카운트와_진루_현황을_함께_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateCurrentHalfInning(MATCH_ID, BaseballHalfInning.FIRST_INNING_TOP);

        ArgumentCaptor<BaseballCount> countCaptor = ArgumentCaptor.forClass(BaseballCount.class);
        ArgumentCaptor<BaseballBaseStates> baseStatesCaptor = ArgumentCaptor.forClass(BaseballBaseStates.class);
        then(baseballMatchDetailRepository).should().updateCurrentHalfInning(
                eq(MATCH_ID),
                eq(BaseballHalfInning.FIRST_INNING_TOP),
                countCaptor.capture(),
                baseStatesCaptor.capture());
        assertThat(countCaptor.getValue().getBalls()).isZero();
        assertThat(countCaptor.getValue().getStrikes()).isZero();
        assertThat(countCaptor.getValue().getOuts()).isZero();
        for (BaseballBase base : BaseballBase.values()) {
            BaseballBaseStates.BaseballBaseState baseState = baseStatesCaptor.getValue().getBaseState(base);
            assertThat(baseState.isRunnerOnBase()).isFalse();
            assertThat(baseState.getRunnerName()).isEmpty();
        }
        then(eventProcessorRegistry).should().process(any(), any());
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 이닝별_점수의_총점이_변경되면_경기_점수와_두_이벤트_처리를_수행한다() {
        Match match = givenBaseballMatch(0, 0);
        BaseballDetail currentDetail = BaseballDetail.builder()
                .inningScores(BaseballInningScores.initialize())
                .build();
        MatchDetailDocument currentDocument = baseballDocument(currentDetail);
        BaseballInningScores newScores = inningScores(Map.of(
                BaseballHalfInning.FIRST_INNING_TOP, 2,
                BaseballHalfInning.FIRST_INNING_BOTTOM, 1));
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(currentDocument));

        service.updateInningScores(MATCH_ID, newScores);

        assertThat(match.getHomeScore()).isEqualTo(1);
        assertThat(match.getAwayScore()).isEqualTo(2);
        then(eventPublisher).should().publishScoreChanged(MATCH_ID, 1, 2);
        then(baseballMatchDetailRepository).should().updateInningScores(MATCH_ID, newScores);

        ArgumentCaptor<MatchDetailDocument> newDocumentCaptor =
                ArgumentCaptor.forClass(MatchDetailDocument.class);
        then(eventProcessorRegistry).should().process(eq(currentDocument), newDocumentCaptor.capture());
        BaseballDetail capturedDetail = (BaseballDetail) newDocumentCaptor.getValue().getSportDetail();
        assertThat(capturedDetail.getInningScores()).isSameAs(newScores);
    }

    @Test
    void 이닝별_점수의_배치만_변경되고_총점이_같으면_공통_점수_이벤트는_발행하지_않는다() {
        givenBaseballMatch(1, 1);
        BaseballDetail currentDetail = BaseballDetail.builder()
                .inningScores(inningScores(Map.of(
                        BaseballHalfInning.FIRST_INNING_TOP, 1,
                        BaseballHalfInning.FIRST_INNING_BOTTOM, 1)))
                .build();
        MatchDetailDocument currentDocument = baseballDocument(currentDetail);
        BaseballInningScores newScores = inningScores(Map.of(
                BaseballHalfInning.FIRST_INNING_TOP, 0,
                BaseballHalfInning.FIRST_INNING_BOTTOM, 0,
                BaseballHalfInning.SECOND_INNING_TOP, 1,
                BaseballHalfInning.SECOND_INNING_BOTTOM, 1));
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(currentDocument));

        service.updateInningScores(MATCH_ID, newScores);

        then(eventPublisher).should(never()).publishScoreChanged(any(), any(), any());
        then(baseballMatchDetailRepository).should().updateInningScores(MATCH_ID, newScores);
        then(eventProcessorRegistry).should().process(eq(currentDocument), any(MatchDetailDocument.class));
    }

    @Test
    void 상세가_없어도_양팀_스탯을_저장한다() {
        BaseballStats homeStats = new BaseballStats(3, 0, 1);
        BaseballStats awayStats = new BaseballStats(2, 1, 0);
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBaseballStats(MATCH_ID, homeStats, awayStats);

        ArgumentCaptor<BaseballStats> homeCaptor = ArgumentCaptor.forClass(BaseballStats.class);
        ArgumentCaptor<BaseballStats> awayCaptor = ArgumentCaptor.forClass(BaseballStats.class);
        then(baseballMatchDetailRepository).should()
                .updateStats(eq(MATCH_ID), homeCaptor.capture(), awayCaptor.capture());
        assertThat(homeCaptor.getValue().getHits()).isEqualTo(3);
        assertThat(homeCaptor.getValue().getErrors()).isZero();
        assertThat(homeCaptor.getValue().getBaseOnBalls()).isEqualTo(1);
        assertThat(awayCaptor.getValue().getHits()).isEqualTo(2);
        assertThat(awayCaptor.getValue().getErrors()).isEqualTo(1);
        assertThat(awayCaptor.getValue().getBaseOnBalls()).isZero();
        then(eventProcessorRegistry).shouldHaveNoInteractions();
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_카운트를_저장한다() {
        BaseballCount count = new BaseballCount(2, 1, 2);
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBaseballCount(MATCH_ID, count);

        then(baseballMatchDetailRepository).should().updateCount(MATCH_ID, count);
        then(eventProcessorRegistry).should().process(any(), any());
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_베이스_상태를_저장한다() {
        BaseballBaseStates baseStates = BaseballBaseStates.of(Map.of(
                BaseballBase.SECOND_BASE, new BaseballBaseStates.BaseballBaseState(true, "주자")));
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBaseStates(MATCH_ID, baseStates);

        then(baseballMatchDetailRepository).should().updateBaseStates(MATCH_ID, baseStates);
        then(eventProcessorRegistry).should().process(any(), any());
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_투수명을_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updatePitcher(MATCH_ID, "투수");

        then(baseballMatchDetailRepository).should().updatePitcher(MATCH_ID, "투수");
        then(eventProcessorRegistry).should().process(any(), any());
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_타자명을_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBatter(MATCH_ID, "타자");

        then(baseballMatchDetailRepository).should().updateBatter(MATCH_ID, "타자");
        then(eventProcessorRegistry).should().process(any(), any());
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_상세_중계를_활성화한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateDetailedBroadcastEnabled(MATCH_ID, true);

        then(baseballMatchDetailRepository).should().updateDetailedBroadcastEnabled(MATCH_ID, true);
        then(eventProcessorRegistry).shouldHaveNoInteractions();
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    private Match givenBaseballMatch(Integer homeScore, Integer awayScore) {
        Match match = Match.builder()
                .id(MATCH_ID)
                .sportId(SPORT_ID)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
        given(sportCodeResolver.resolveByInternalSportId(SPORT_ID)).willReturn(SportCode.BASEBALL);
        return match;
    }

    private BaseballInningScores inningScores(Map<BaseballHalfInning, Integer> values) {
        EnumMap<BaseballHalfInning, Integer> scores = new EnumMap<>(BaseballHalfInning.class);
        scores.putAll(values);
        return BaseballInningScores.of(scores);
    }

    private void givenMatchSport(SportCode sportCode) {
        Match match = Match.builder()
                .id(MATCH_ID)
                .sportId(SPORT_ID)
                .build();
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
        given(sportCodeResolver.resolveByInternalSportId(SPORT_ID)).willReturn(sportCode);
    }

    private MatchDetailDocument baseballDocument(BaseballDetail baseballDetail) {
        return MatchDetailDocument.builder()
                .id(MATCH_ID)
                .type(SportCode.BASEBALL)
                .sportDetail(baseballDetail)
                .build();
    }
}
