package com.scorenow.scorenow_api.domain.match.dto;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.*;
import lombok.EqualsAndHashCode.Include;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MatchCandidate {
    @Include
    private Long matchId;
    private Long sportId;

    private DataOrigin provider;
    private String apiMatchId;
    private String apiSportId;

    private LocalDateTime startAt;
}
