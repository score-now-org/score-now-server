package com.scorenow.scorenow_api.domain.stadium.entity;

import lombok.*;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class StadiumCacheKey {
    @NonNull
    private final String sportId;
    @NonNull
    private final String externalStadiumId;

    public static StadiumCacheKey generateKeyFrom(@NonNull Stadium stadium) {
        String sportId = stadium.getSportId();
        String externalStadiumId = stadium.getExternalStadiumId();

        return new StadiumCacheKey(sportId, externalStadiumId);
    }
}
