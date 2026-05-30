package com.scorenow.scorenow_api.domain.match.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchListResponse {
    private Long id;
    private Long sportId;
    private String sportName;
    private Long leagueId;
    private String leagueName;
    private String matchType;
    private String statusCode;
    private String statusName;
    private Long homeId;
    private String homeName;
    private String homeImageUrl;
    private Integer homeScore;
    private Long awayId;
    private String awayName;
    private String awayImageUrl;
    private Integer awayScore;
    private boolean isManual;
    private boolean isActive;
    private List<String> teamDisplayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
}
