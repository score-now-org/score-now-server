package com.scorenow.scorenow_api.domain.player.batch.dto;

import java.util.List;

import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;

/** 저장 결과 데이터 */
public record PlayerSyncData(
	Long sportId,
	Long leagueId,
	String leagueApiId,
	String teamApiId,
	List<BetsSquadResponse.SquadPlayer> players
) {
}
