package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.external.common.ExternalProvider;
import lombok.*;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class StadiumCacheKey {
    @NonNull
    private final ExternalProvider provider;
    @NonNull
    private final String externalSportId;
    @NonNull
    private final String externalStadiumId;
}
