package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchRealtimeEvent<T> {
    private String type;
    private Long matchId;
    private T data;

    public static <T> MatchRealtimeEvent<T> of(MatchRealtimeEventType eventType, Long matchId, T data) {
        return MatchRealtimeEvent.<T>builder()
                .type(eventType.name())
                .matchId(matchId)
                .data(data)
                .build();
    }
}
