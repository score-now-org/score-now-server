package com.scorenow.scorenow_api.domain.match.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Map<SseEmitter, Set<Long>> matchIdsByEmitter = new ConcurrentHashMap<>();

    public SseEmitter register(List<Long> matchIds) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        Set<Long> uniqueMatchIds = ConcurrentHashMap.newKeySet();
        uniqueMatchIds.addAll(matchIds);

        // 1. Emitter 가 구독하는 MatchIds 추가
        matchIdsByEmitter.put(emitter, uniqueMatchIds);

        // 2. MatchId 를 구독하는 Emitter 추가
        for (Long uniqueMatchId : uniqueMatchIds) {
            emittersByMatchId
                    .computeIfAbsent(uniqueMatchId, key -> ConcurrentHashMap.newKeySet())
                    .add(emitter);
        }

        emitter.onCompletion(() -> {
            if (remove(emitter)) {
                log.info("🟢SSE 연결 종료. matchIds={}", uniqueMatchIds);
            }
        });

        emitter.onTimeout(() -> {
            if (remove(emitter)) {
                log.info("🟠️SSE 연결 타임아웃 발생. matchIds={}", uniqueMatchIds);
            }
        });

        emitter.onError(e -> {
            if (remove(emitter)) {
                log.warn("🔴SSE 연결 에러 발생. matchIds={}", uniqueMatchIds);
            }
        });

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
                log.warn("🔴SSE 전송 실패. matchId={}, eventName={}", matchId, eventName);
                remove(emitter);
            }
        }
    }

    public void sendPingToAll() {
        Set<SseEmitter> emitters = matchIdsByEmitter.keySet();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(PING.name())
                        .data(Map.of(
                                "type", PING.name()
                        )));
            } catch (IOException | IllegalStateException e) {
                log.warn("🔴SSE 전송 실패. eventName={}", PING.name());
                remove(emitter);
            }
        }
    }

    private boolean remove(SseEmitter emitter) {
        // 1. Emitter 를 제거한다.
        Set<Long> matchIds = matchIdsByEmitter.remove(emitter);

        if (matchIds == null) {
            return false;
        }

        // 2. 각 경기들을 구독하고 있는 Emitter 들 중에서 방금 제거된 Emitter 를 찾아서 제거한다.
        for (Long matchId : matchIds) {
            Set<SseEmitter> emitters = emittersByMatchId.get(matchId);

            // 경기를 구독하고 있는 Emitter 가 없는 경우 다음 경기를 확인한다.
            if (emitters == null) {
                continue;
            }

            // Emitter 를 제거한다.
            emitters.remove(emitter);

            // Emitter 제거 후, 더 이상 해당 경기를 구독하고 있는 Emitter 가 없는 경우 Map 에서 지워준다.
            if (emitters.isEmpty()) {
                emittersByMatchId.remove(matchId);
            }
        }

        return true;
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
            log.warn("🔴SSE 전송 실패. matchIds={}, eventName={}", matchIds, CONNECTED.name());
            remove(emitter);
        }
    }
}
