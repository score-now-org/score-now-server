package com.scorenow.scorenow_api.domain.match.realtime;

import com.scorenow.scorenow_api.domain.match.dto.sse.MatchCommentaryChangedPayload;
import com.scorenow.scorenow_api.domain.match.dto.sse.MatchRealtimeEvent;
import com.scorenow.scorenow_api.domain.match.dto.sse.MatchScoreChangedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.COMMENTARY_CHANGED;
import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.SCORE_CHANGED;

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

}
