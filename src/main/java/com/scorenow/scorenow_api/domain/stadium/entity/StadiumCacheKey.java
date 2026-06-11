package com.scorenow.scorenow_api.domain.stadium.entity;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.*;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class StadiumCacheKey {
    @NonNull
    private final DataOrigin provider;
    @NonNull
    private final String apiSportId;
    @NonNull
    private final String apiStadiumId;
}
