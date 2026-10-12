package com.scorenow.scorenow_api.domain.match.dto.sync;

public record MatchDetailSyncStadium(
        String externalId,
        String name,
        String city
) {
}
