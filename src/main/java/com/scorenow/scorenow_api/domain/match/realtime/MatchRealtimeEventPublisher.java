package com.scorenow.scorenow_api.domain.match.realtime;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.sse.*;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.service.MatchDisplayTextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.*;

@Component
@RequiredArgsConstructor
public class MatchRealtimeEventPublisher {

    private final MatchSseEmitterRegistry registry;
    private final MatchDisplayTextResolver matchDisplayTextResolver;

    /**
     * 중계 멘트 변경 이벤트 발행
     */
    public void publishCommentaryChanged(Long matchId, String commentaryId, String content, String imageUrl) {
        MatchCommentaryChangedPayload payload = MatchCommentaryChangedPayload.builder()
                .commentaryId(commentaryId)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        MatchRealtimeEvent<MatchCommentaryChangedPayload> data =
                MatchRealtimeEvent.of(COMMENTARY_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, COMMENTARY_CHANGED.name(), data);
    }

    /**
     * 경기 점수 변경 이벤트 발행
     */
    public void publishScoreChanged(
            Long matchId,
            Integer homeScore,
            Integer awayScore,
            Integer homeShootOutScore,
            Integer awayShootOutScore) {

        MatchScoreChangedPayload payload = MatchScoreChangedPayload.builder()
                .homeScore(homeScore)
                .awayScore(awayScore)
                .homeShootOutScore(homeShootOutScore)
                .awayShootOutScore(awayShootOutScore)
                .build();

        MatchRealtimeEvent<MatchScoreChangedPayload> data =
                MatchRealtimeEvent.of(SCORE_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, SCORE_CHANGED.name(), data);
    }

    /**
     * 경기 상태 변경 이벤트 발행 (경기전 -> 경기중)
     */
    public void publishMatchStatusChanged(Long matchId, MatchStatus matchStatus) {
        MatchStatusChangedPayload payload = MatchStatusChangedPayload.builder()
                .statusCode(matchStatus.name())
                .statusName(matchStatus.getDescription())
                .build();

        MatchRealtimeEvent<MatchStatusChangedPayload> data =
                MatchRealtimeEvent.of(MATCH_STATUS_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, MATCH_STATUS_CHANGED.name(), data);
    }

    /**
     * 경기 상태 변경 이벤트 발행 (경기중 -> 경기종료)
     */
    public void publishMatchStatusChanged(Long matchId, MatchStatus matchStatus, MatchResult matchResult) {
        boolean ended = MatchAppStatusGroup.findBy(matchStatus).isPresent();

        MatchStatusChangedPayload payload = MatchStatusChangedPayload.builder()
                .statusCode(matchStatus.name())
                .statusName(matchStatus.getDescription())
                .displayText(ended ?
                        matchDisplayTextResolver.resolveEndedDisplayText(matchResult) :
                        null)
                .build();

        MatchRealtimeEvent<MatchStatusChangedPayload> data =
                MatchRealtimeEvent.of(MATCH_STATUS_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, MATCH_STATUS_CHANGED.name(), data);
    }

    /**
     * 경기 시간 변경 이벤트 발행
     * [이벤트 발행 조건]
     * 1. 경기 시간 정보가 새로 생길 때
     * 2. period 가 바뀔 때
     * 3. running 상태가 바뀔 때
     * 4. 수동 수정으로 시간 정보가 바뀔 때
     */
    public void publishMatchClockChanged(Long matchId, MatchDetailDocument.MatchClock newMatchClock) {
        if (newMatchClock == null) {
            return;
        }

        String displayText = matchDisplayTextResolver.resolveInPlayDisplayText(newMatchClock);
        Integer displayElapsedMinutes = matchDisplayTextResolver.resolvePeriodElapsedMinutes(newMatchClock);

        MatchClockChangedPayload payload = MatchClockChangedPayload.from(newMatchClock, displayText, displayElapsedMinutes);

        MatchRealtimeEvent<MatchClockChangedPayload> data = MatchRealtimeEvent.of(MATCH_CLOCK_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, MATCH_CLOCK_CHANGED.name(), data);
    }

}
