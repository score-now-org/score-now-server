package com.scorenow.scorenow_api.domain.match.realtime;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.CONNECTED;
import static com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventType.PING;

@Slf4j
@Component
public class MatchSseEmitterRegistry {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분

    // 특정 matchId 를 구독하는 Emitter 를 저장하기 위한 용도
    private final Map<Long, Set<SseEmitter>> emittersByMatchId = new ConcurrentHashMap<>();

    // Emitter 가 구독하고 있는 matchId 목록을 저장하기 위한 용도
    private final Map<SseEmitter, SseConnection> connectionsByEmitter = new ConcurrentHashMap<>();

    public SseEmitter register(List<Long> matchIds) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        Set<Long> uniqueMatchIds = ConcurrentHashMap.newKeySet();
        uniqueMatchIds.addAll(matchIds);

        SseConnection connection = new SseConnection(
                UUID.randomUUID().toString(),
                emitter,
                uniqueMatchIds,
                Instant.now()
        );

        // 1. Emitter 가 구독하는 connection 추가
        connectionsByEmitter.put(emitter, connection);

        // 2. MatchId 를 구독하는 Emitter 추가
        for (Long matchId : uniqueMatchIds) {
            emittersByMatchId.computeIfAbsent(matchId, newMatchId -> ConcurrentHashMap.newKeySet())
                    .add(emitter);
        }

        log.info("🟢SSE_OPEN connectionId={} matchIds={} activeConnections={}",
                connection.getConnectionId(),
                connection.sortedMatchIds(),
                connectionsByEmitter.size());

        emitter.onCompletion(() -> deregisterAndLog(emitter, "completion", null));
        emitter.onTimeout(() -> deregisterAndLog(emitter, "application_timeout", null));
        emitter.onError(error -> deregisterAndLog(emitter, "error", error));

        // 3. 연결 완료 이벤트 전송
        sendConnectedEvent(emitter, uniqueMatchIds);

        return emitter;
    }

    public void sendToMatch(Long matchId, String eventName, Object data) {
        Set<SseEmitter> emitters = emittersByMatchId.getOrDefault(matchId, Collections.emptySet());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException | IllegalStateException e) {
                deregisterAndLog(emitter, "send_failed:" + eventName, e);
            }
        }
    }

    public void sendPingToAll() {
        Set<SseEmitter> emitters = connectionsByEmitter.keySet();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(PING.name())
                        .data(Map.of(
                                "type", PING.name()
                        )));
            } catch (IOException | IllegalStateException e) {
                deregisterAndLog(emitter, "send_failed:" + PING.name(), e);
            }
        }
    }

    private void sendConnectedEvent(SseEmitter emitter, Set<Long> matchIds) {
        try {
            emitter.send(SseEmitter.event()
                    .name(CONNECTED.name())
                    .data(Map.of(
                            "type", CONNECTED.name(),
                            "matchIds", matchIds
                    )));
        } catch (IOException | IllegalStateException e) {
            deregisterAndLog(emitter, "send_failed:" + CONNECTED.name(), e);
        }
    }

    private boolean deregisterAndLog(SseEmitter emitter, String reason, Throwable error) {
        SseConnection connection = connectionsByEmitter.remove(emitter);

        // 연결 정상 종료, 연결 예외, 전송 실패 경우에 대해서 중복 호출을 막기 위함
        if (connection == null) {
            return false;
        }

        // 특정 경기를 구독하고 있는 Emitters 에서 지워야 하는 emitter 를 삭제한다.
        for (Long matchId : connection.getMatchIds()) {
            emittersByMatchId.computeIfPresent(matchId, (key, emitters) -> {
                emitters.remove(emitter);
                return emitters.isEmpty() ? null : emitters;
            });
        }

        long durationMs = Duration.between(connection.getConnectedAt(), Instant.now()).toMillis();

        if (error == null) {
            log.info("🟠SSE_CLOSE connectionId={} reason={} durationMs={} matchIds={} activeConnections={}",
                    connection.getConnectionId(),
                    reason,
                    durationMs,
                    connection.sortedMatchIds(),
                    connectionsByEmitter.size());
        } else {
            log.warn(
                    "🔴SSE_CLOSE connectionId={} reason={} durationMs={} matchIds={} errorType={} errorMessage={} activeConnections={}",
                    connection.getConnectionId(),
                    reason,
                    durationMs,
                    connection.sortedMatchIds(),
                    error.getClass().getName(),
                    error.getMessage(),
                    connectionsByEmitter.size(),
                    error
            );
        }

        return true;
    }


    @Getter
    private static class SseConnection {
        private final String connectionId;
        private final SseEmitter emitter;
        private final Set<Long> matchIds;
        private final Instant connectedAt;

        public SseConnection(String connectionId, SseEmitter emitter, Set<Long> matchIds, Instant connectedAt) {
            this.connectionId = connectionId;
            this.emitter = emitter;
            this.matchIds = matchIds;
            this.connectedAt = connectedAt;
        }

        private List<Long> sortedMatchIds() {
            return matchIds.stream()
                    .sorted()
                    .toList();
        }
    }
}
