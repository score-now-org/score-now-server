package com.scorenow.scorenow_api.domain.league.service.standings.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TeamStandingSummary {
    private final String groupName;
    private final Integer groupOrder;
    private final Integer rank;
}
