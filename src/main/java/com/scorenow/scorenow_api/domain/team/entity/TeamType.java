package com.scorenow.scorenow_api.domain.team.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamType {
    CLUB("클럽"),
    NATIONAL("국가대표");

    private final String displayName;
}
