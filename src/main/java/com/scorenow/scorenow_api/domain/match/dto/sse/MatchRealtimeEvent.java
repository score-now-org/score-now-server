package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MatchRealtimeEvent<T> {
    private String type;
    private Long matchId;
    private Instant publishedAt;
    private T data;

    public static <T> MatchRealtimeEvent<T> of(MatchRealtimeEventType eventType, Long matchId, T data) {
        return MatchRealtimeEvent.<T>builder()
                .type(eventType.name())
                .matchId(matchId)
                .publishedAt(Instant.now())
                .data(data)
                .build();
    }
}
