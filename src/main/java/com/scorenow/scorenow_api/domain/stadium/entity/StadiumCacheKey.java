package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import lombok.*;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class StadiumCacheKey {
    @NonNull
    private final ApiProvider provider;
    @NonNull
    private final String apiSportId;
    @NonNull
    private final String apiStadiumId;
}
