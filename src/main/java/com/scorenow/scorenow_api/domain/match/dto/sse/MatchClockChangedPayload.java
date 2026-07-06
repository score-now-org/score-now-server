package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MatchClockChangedPayload {
    private String displayText;
    private Integer elapsedMinutes;
    private Integer displayElapsedMinutes;
    private Integer elapsedSeconds;
    private String periodCode;
    private String periodName;
    private Boolean running;
    private Instant providerUpdatedAt;

    public static MatchClockChangedPayload from(MatchDetailDocument.MatchClock matchClock, String displayText, Integer displayElapsedMinutes) {
        MatchPeriod period = matchClock.getPeriod();

        return MatchClockChangedPayload.builder()
                .displayText(displayText)
                .elapsedMinutes(matchClock.getElapsedMinutes())
                .displayElapsedMinutes(displayElapsedMinutes)
                .elapsedSeconds(matchClock.getElapsedSeconds())
                .periodCode(period != null ? period.name() : null)
                .periodName(period != null ? period.getDescription() : null)
                .running(matchClock.getRunning())
                .providerUpdatedAt(matchClock.getProviderUpdatedAt())
                .build();
    }

}
