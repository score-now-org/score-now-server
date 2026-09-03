package com.scorenow.scorenow_api.domain.match.realtime;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import com.scorenow.scorenow_api.domain.match.dto.sse.*;
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
    public void publishCommentaryChanged(Long matchId, String commentaryId, String content, boolean highlighted, String imageUrl) {
        MatchCommentaryChangedPayload payload = MatchCommentaryChangedPayload.builder()
                .commentaryId(commentaryId)
                .content(content)
                .highlighted(highlighted)
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
            Integer awayScore) {

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
    public void publishMatchStatusChanged(Long matchId, MatchStatus matchStatus, String displayText) {

        MatchStatusChangedPayload payload = MatchStatusChangedPayload.builder()
                .statusCode(matchStatus.name())
                .statusName(matchStatus.getDescription())
                .displayText(displayText)
                .build();

        MatchRealtimeEvent<MatchStatusChangedPayload> data =
                MatchRealtimeEvent.of(MATCH_STATUS_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, MATCH_STATUS_CHANGED.name(), data);
    }

    /**
     * 축구 경기 시간 변경 이벤트 발행
     * [이벤트 발행 조건]
     * 1. 축구 경기 시간 정보가 새로 생길 때
     * 2. Phase 가 바뀔 때
     * 3. running 상태가 바뀔 때
     * 4. 관리자 수동 시간 보정
     */
    public void publishFootballClockChanged(Long matchId, FootballClock newFootballClock, String displayText) {
        if (newFootballClock == null) {
            return;
        }

        FootballClockChangedPayload payload = FootballClockChangedPayload.from(newFootballClock, displayText);

        MatchRealtimeEvent<FootballClockChangedPayload> data = MatchRealtimeEvent.of(FOOTBALL_CLOCK_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, FOOTBALL_CLOCK_CHANGED.name(), data);
    }

    /**
     * 축구 승부차기 스코어 변경 이벤트 발행
     */
    public void publishFootballShootOutScoreChanged(Long matchId, FootballShootOutScore newShootOutScore) {
        if (newShootOutScore == null) {
            return;
        }

        FootballShootOutScoreChangePayload payload = FootballShootOutScoreChangePayload.builder()
                .homeShootOutScore(newShootOutScore.getHomeScore())
                .awayShootOutScore(newShootOutScore.getAwayScore())
                .build();

        MatchRealtimeEvent<FootballShootOutScoreChangePayload> data = MatchRealtimeEvent.of(FOOTBALL_SHOOT_OUT_SCORE_CHANGED, matchId, payload);

        registry.sendToMatch(matchId, FOOTBALL_SHOOT_OUT_SCORE_CHANGED.name(), data);
    }
}
