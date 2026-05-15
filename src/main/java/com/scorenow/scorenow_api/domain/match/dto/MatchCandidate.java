package com.scorenow.scorenow_api.domain.match.dto;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MatchCandidate {
    @Include
    private final Long matchId;
    private final Long sportId;

    private final ApiProvider provider;
    private final String apiMatchId;
    private final String apiSportId;

    private final LocalDateTime startAt;
}
