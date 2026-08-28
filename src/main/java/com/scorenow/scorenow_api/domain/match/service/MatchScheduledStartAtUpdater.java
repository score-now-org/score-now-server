package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MatchScheduledStartAtUpdater {

    private final AdminFeaturedMatchService adminFeaturedMatchService;

    /**
     * 경기 시작일자 변경 시, 상단고정/핫매치를 해제한다.
     */
    public void update(Match match, LocalDateTime newStartAt) {
        if (Objects.equals(match.getStartAt(), newStartAt)) {
            return;
        }

        if (match.isStartDateChanged(newStartAt)) {
            adminFeaturedMatchService.deleteFeaturedMatchByMatchId(match.getId());
        }

        match.updateStartAt(newStartAt);
    }
}
