package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
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
    void 상세가_없어도_이닝과_초기화된_카운트를_함께_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateCurrentHalfInning(MATCH_ID, BaseballHalfInning.FIRST_INNING_TOP);

        ArgumentCaptor<BaseballCount> countCaptor = ArgumentCaptor.forClass(BaseballCount.class);
        then(baseballMatchDetailRepository).should().updateCurrentHalfInning(
                eq(MATCH_ID), eq(BaseballHalfInning.FIRST_INNING_TOP), countCaptor.capture());
        assertThat(countCaptor.getValue().getBalls()).isZero();
        assertThat(countCaptor.getValue().getStrikes()).isZero();
        assertThat(countCaptor.getValue().getOuts()).isZero();
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
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
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_카운트를_저장한다() {
        BaseballCount count = new BaseballCount(2, 1, 2);
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBaseballCount(MATCH_ID, count);

        then(baseballMatchDetailRepository).should().updateCount(MATCH_ID, count);
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
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_투수명을_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updatePitcher(MATCH_ID, "투수");

        then(baseballMatchDetailRepository).should().updatePitcher(MATCH_ID, "투수");
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_타자명을_저장한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateBatter(MATCH_ID, "타자");

        then(baseballMatchDetailRepository).should().updateBatter(MATCH_ID, "타자");
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
    }

    @Test
    void 상세가_없어도_상세_중계를_활성화한다() {
        givenMatchSport(SportCode.BASEBALL);
        given(matchDetailRepository.findById(MATCH_ID)).willReturn(Optional.of(baseballDocument(null)));

        service.updateDetailedBroadcastEnabled(MATCH_ID, true);

        then(baseballMatchDetailRepository).should().updateDetailedBroadcastEnabled(MATCH_ID, true);
        then(matchDetailRepository).should(never()).upsertMatchDetail(any());
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
