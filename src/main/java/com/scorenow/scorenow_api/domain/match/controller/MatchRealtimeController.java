package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.realtime.MatchSseEmitterRegistry;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/matches")
public class MatchRealtimeController {

    private static final int MAX_SUBSCRIBE_MATCH_COUNT = 100;

    private final MatchSseEmitterRegistry registry;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam List<Long> matchIds) {
        validateMatchIds(matchIds);

        List<Long> uniqueMatchIds = matchIds.stream()
                .distinct()
                .toList();

        return registry.register(uniqueMatchIds);
    }

    private void validateMatchIds(List<Long> matchIds) {
        // 1. 값 존재 여부 검증
        if (matchIds == null || matchIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        // 2. ID가 0 또는 음수 여부 검증
        boolean hasInvalidMatchId = matchIds.stream()
                .anyMatch(matchId -> matchId == null || matchId <= 0);

        if (hasInvalidMatchId) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        // 3. 최대 구독 경기 수 검증
        if (matchIds.size() > MAX_SUBSCRIBE_MATCH_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

}
