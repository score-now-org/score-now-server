package com.scorenow.scorenow_api.domain.match.realtime;

import com.scorenow.scorenow_api.domain.match.dto.sse.MatchCommentaryChangedPayload;
import com.scorenow.scorenow_api.domain.match.dto.sse.MatchRealtimeEvent;
import com.scorenow.scorenow_api.domain.match.dto.sse.MatchScoreChangedPayload;
import com.scorenow.scorenow_api.domain.match.dto.sse.MatchStatusChangedPayload;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.*;

@Component
@RequiredArgsConstructor
public class MatchRealtimeEventPublisher {

    private final MatchSseEmitterRegistry registry;

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
    public void publishScoreChanged(Long matchId, Integer homeScore, Integer awayScore) {
        MatchScoreChangedPayload payload = MatchScoreChangedPayload.builder()
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();

        MatchRealtimeEvent<MatchScoreChangedPayload> data =
                MatchRealtimeEvent.of(SCORE_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, SCORE_CHANGED.name(), data);
    }

    /**
     * 경기 상태 변경 이벤트 발행
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

}
