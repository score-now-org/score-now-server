package com.scorenow.scorenow_api.domain.match.model;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum MatchAppStatusGroup {
    IN_PLAY(0, List.of(
            MatchStatus.IN_PLAY,
            MatchStatus.INTERRUPTED
    )),
    SCHEDULED(1, List.of(
            MatchStatus.NOT_STARTED
    )),
    ENDED(2, List.of(
            MatchStatus.ENDED,
            MatchStatus.POSTPONED,
            MatchStatus.CANCELLED,
            MatchStatus.ABANDONED
    ));

    private final int priority;
    private final List<MatchStatus> statuses;

    MatchAppStatusGroup(int priority, List<MatchStatus> statuses) {
        this.priority = priority;
        this.statuses = List.copyOf(statuses);
    }

    public static MatchAppStatusGroup from(MatchStatus status) {
        return findBy(status)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 상태 값입니다: " + status));
    }

    public static Optional<MatchAppStatusGroup> findBy(MatchStatus status) {
        if (status == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(group -> group.contains(status))
                .findFirst();
    }

    public static List<MatchStatus> allStatuses() {
        return Arrays.stream(values())
                .flatMap(group -> group.statuses.stream())
                .distinct()
                .toList();
    }

    public boolean contains(MatchStatus status) {
        return statuses.contains(status);
    }
}
