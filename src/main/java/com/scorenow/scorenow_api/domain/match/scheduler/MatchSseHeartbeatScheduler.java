package com.scorenow.scorenow_api.domain.match.scheduler;

import com.scorenow.scorenow_api.domain.match.realtime.MatchSseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSseHeartbeatScheduler {

    private final MatchSseEmitterRegistry registry;

    /**
     * 작업 내용 : 모든 SSE 연결에 PING 을 보낸다.
     * 작업 주기 : 운영 프록시 idle timeout 보다 짧은 간격
     */
    @Scheduled(fixedDelay = 30_000, zone = "Asia/Seoul")
    public void sendHeartbeat() {
        log.debug("📅SSE heartbeat 전송");
        registry.sendPingToAll();
    }
}
