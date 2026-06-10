package com.scorenow.scorenow_api.domain.player.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LeaguePlayerSyncResult {

	private final Long leagueId; // League.id
	private final String leagueApiId; // LeagueExternalMapping.apiLeagueId
	private final String seasonName;
	private final int totalTeamCount;

	private int successTeamCount;
	private int failedTeamCount;
	private int processedPlayerCount;

	private final List<String> failedTeamApiIds = new ArrayList<>();

	public void addSuccessTeam(int processedCount) {
		this.successTeamCount++;
		this.processedPlayerCount += processedCount;
	}

	public void addFailedTeam(String teamApiId) {
		this.failedTeamCount++;
		this.failedTeamApiIds.add(teamApiId);
	}
}
