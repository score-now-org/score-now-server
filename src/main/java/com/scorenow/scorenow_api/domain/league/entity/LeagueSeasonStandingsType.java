package com.scorenow.scorenow_api.domain.league.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeagueSeasonStandingsType {
    IMAGE("이미지"),
    EXTERNAL_DATA("외부 데이터");

    private final String description;
}
