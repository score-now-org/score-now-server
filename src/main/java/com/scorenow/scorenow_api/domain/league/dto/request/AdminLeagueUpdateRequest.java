package com.scorenow.scorenow_api.domain.league.dto.request;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLeagueUpdateRequest {

	private Long sportId;

	private String eName;

	private String kName;

	private String sName;

	private TeamDisplayOrder teamDisplayOrder;
}
