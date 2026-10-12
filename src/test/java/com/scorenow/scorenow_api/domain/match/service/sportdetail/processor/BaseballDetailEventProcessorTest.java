package com.scorenow.scorenow_api.domain.match.service.sportdetail.processor;

import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase.FIRST_BASE;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.FIRST_INNING_BOTTOM;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.FIRST_INNING_TOP;
import static com.scorenow.scorenow_api.domain.sport.model.SportCode.BASEBALL;
import static org.mockito.BDDMockito.then;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates.BaseballBaseState;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseballDetailEventProcessorTest {

    private static final long MATCH_ID = 1L;

    @InjectMocks
    private BaseballDetailEventProcessor processor;

    @Mock
    private MatchRealtimeEventPublisher eventPublisher;

    @Test
    void 변경이_없으면_이벤트를_발행하지_않는다() {
        BaseballDetail currentDetail = baseballDetail(true);
        BaseballDetail newDetail = baseballDetail(true);

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 현재_이닝이_변경되면_이벤트를_발행한다() {
        BaseballDetail currentDetail = baseballDetail(false);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .currentHalfInning(FIRST_INNING_BOTTOM)
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 이닝별_점수가_변경되면_이벤트를_발행한다() {
        BaseballDetail currentDetail = baseballDetail(false);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .inningScores(inningScores(Map.of(
                        FIRST_INNING_TOP, 1,
                        FIRST_INNING_BOTTOM, 0)))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 상세_중계가_활성화되면_투수나_타자_변경을_발행한다() {
        BaseballDetail currentDetail = baseballDetail(true);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .pitcher("new pitcher")
                .batter("new batter")
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 상세_중계가_활성화되면_주자_변경을_발행한다() {
        BaseballDetail currentDetail = baseballDetail(true);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .baseStates(baseStates(true, "runner"))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 상세_중계가_활성화되면_카운트_변경을_발행한다() {
        BaseballDetail currentDetail = baseballDetail(true);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .count(new BaseballCount(1, 1, 1))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 상세_중계가_비활성화되면_상세_정보가_변경되어도_이벤트를_발행하지_않는다() {
        BaseballDetail currentDetail = baseballDetail(false);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .pitcher("new pitcher")
                .batter("new batter")
                .baseStates(baseStates(true, "runner"))
                .count(new BaseballCount(1, 1, 1))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void HEB만_변경되면_이벤트를_발행하지_않는다() {
        BaseballDetail currentDetail = baseballDetail(true);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .homeStats(new BaseballStats(2, 1, 1))
                .awayStats(new BaseballStats(3, 2, 1))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 상세_중계_설정만_변경되면_이벤트를_발행하지_않는다() {
        BaseballDetail currentDetail = baseballDetail(false);
        BaseballDetail newDetail = currentDetail.toBuilder()
                .detailedBroadcastEnabled(true)
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void 초기_null_이닝별_점수에_값이_생기면_이벤트를_발행한다() {
        BaseballDetail currentDetail = BaseballDetail.empty();
        BaseballDetail newDetail = currentDetail.toBuilder()
                .inningScores(inningScores(Map.of(FIRST_INNING_TOP, 1)))
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    @Test
    void 상세_중계_활성화_상태에서_null_주자와_카운트에_값이_생겨도_정상적으로_발행한다() {
        BaseballDetail currentDetail = BaseballDetail.builder()
                .detailedBroadcastEnabled(true)
                .build();
        BaseballDetail newDetail = currentDetail.toBuilder()
                .baseStates(BaseballBaseStates.initialize())
                .count(BaseballCount.empty())
                .build();

        processor.process(document(currentDetail), document(newDetail));

        then(eventPublisher).should().publishBaseballDetailChanged(MATCH_ID, newDetail);
    }

    private MatchDetailDocument document(BaseballDetail baseballDetail) {
        return MatchDetailDocument.builder()
                .id(MATCH_ID)
                .type(BASEBALL)
                .sportDetail(baseballDetail)
                .build();
    }

    private BaseballDetail baseballDetail(boolean detailedBroadcastEnabled) {
        return BaseballDetail.builder()
                .currentHalfInning(FIRST_INNING_TOP)
                .inningScores(inningScores(Map.of(
                        FIRST_INNING_TOP, 0,
                        FIRST_INNING_BOTTOM, 0)))
                .detailedBroadcastEnabled(detailedBroadcastEnabled)
                .pitcher("pitcher")
                .batter("batter")
                .baseStates(BaseballBaseStates.initialize())
                .count(BaseballCount.empty())
                .homeStats(new BaseballStats(1, 0, 0))
                .awayStats(new BaseballStats(1, 0, 0))
                .build();
    }

    private BaseballInningScores inningScores(Map<BaseballHalfInning, Integer> values) {
        EnumMap<BaseballHalfInning, Integer> scores = new EnumMap<>(BaseballHalfInning.class);
        scores.putAll(values);
        return BaseballInningScores.of(scores);
    }

    private BaseballBaseStates baseStates(boolean runnerOnBase, String runnerName) {
        EnumMap<BaseballBase, BaseballBaseState> states = new EnumMap<>(BaseballBase.class);
        states.put(FIRST_BASE, new BaseballBaseState(runnerOnBase, runnerName));
        return BaseballBaseStates.of(states);
    }
}
