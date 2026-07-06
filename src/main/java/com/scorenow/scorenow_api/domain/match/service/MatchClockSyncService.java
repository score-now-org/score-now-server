package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchClockSyncService {

    private final MatchDetailRepository matchDetailRepository;
    private final MatchRealtimeEventPublisher eventPublisher;

    /**
     * 경기 시간 자동 동기화
     */
    public void syncMatchClock(Long matchId, MatchDetailDocument.MatchClock newMatchClock) {
        syncMatchClock(matchId, newMatchClock, MatchClockSyncType.AUTO);
    }

    /**
     * 경기 시간 수동 동기화
     */
    public void syncManualMatchClock(Long matchId, MatchDetailDocument.MatchClock newMatchClock) {
        syncMatchClock(matchId, newMatchClock, MatchClockSyncType.MANUAL);
    }

    private void syncMatchClock(Long matchId, MatchDetailDocument.MatchClock newMatchClock, MatchClockSyncType syncType) {
        // 1. 새로운 경기 시간 정보가 없다면 동기화 할 필요가 없으니 종료
        if (newMatchClock == null) {
            return;
        }

        // 2. 기존 경기 시간 정보 조회, 없다면 null 로 조회
        MatchDetailDocument.MatchClock currentMatchClock = matchDetailRepository.findById(matchId)
                .map(MatchDetailDocument::getMatchClock)
                .orElse(null);

        // 3. MatchClock 정보를 upsert 진행
        matchDetailRepository.upsertMatchClock(matchId, newMatchClock);

        // 4. 경기 시간 변경 이벤트 발행
        if (shouldPublishEvent(currentMatchClock, newMatchClock, syncType)) {
            eventPublisher.publishMatchClockChanged(matchId, newMatchClock);
        }
    }

    private boolean shouldPublishEvent(MatchDetailDocument.MatchClock currentMatchClock,
                                       MatchDetailDocument.MatchClock newMatchClock,
                                       MatchClockSyncType syncType) {
        // 기존에 경기 시간 정보가 없었던 경우 이벤트 발행
        if (currentMatchClock == null) {
            return true;
        }

        // 수동으로 경기 시간을 조정하는 경우에 대해서 이벤트 발행
        if (syncType == MatchClockSyncType.MANUAL) {
            return hasClockChanged(currentMatchClock, newMatchClock) ||
                    hasPeriodOrRunningChanged(currentMatchClock, newMatchClock);
        }

        return hasPeriodOrRunningChanged(currentMatchClock, newMatchClock);
    }

    /**
     * 기존 경기 시간 정보와 새로운 경기 시간 정보를 비교하여 이벤트 발행 여부 결정 (경과 시간, 추가 시간)
     */
    private boolean hasClockChanged(
            MatchDetailDocument.MatchClock currentMatchClock,
            MatchDetailDocument.MatchClock newMatchClock) {

        return !Objects.equals(currentMatchClock.getElapsedMinutes(), newMatchClock.getElapsedMinutes()) ||
                !Objects.equals(currentMatchClock.getElapsedSeconds(), newMatchClock.getElapsedSeconds()) ||
                !Objects.equals(currentMatchClock.getAdditionalMinutes(), newMatchClock.getAdditionalMinutes());
    }

    /**
     * 기존 경기 시간 정보와 새로운 경기 시간 정보를 비교하여 이벤트 발행 여부 결정 (period, 경기 진행 여부)
     */
    private boolean hasPeriodOrRunningChanged(
            MatchDetailDocument.MatchClock currentMatchClock,
            MatchDetailDocument.MatchClock newMatchClock) {

        return !Objects.equals(currentMatchClock.getPeriod(), newMatchClock.getPeriod()) ||
                !Objects.equals(currentMatchClock.getRunning(), newMatchClock.getRunning());
    }

    private enum MatchClockSyncType {
        AUTO, MANUAL
    }
}
