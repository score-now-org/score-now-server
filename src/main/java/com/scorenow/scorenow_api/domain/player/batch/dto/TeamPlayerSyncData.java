package com.scorenow.scorenow_api.domain.player.batch.dto;

import java.util.List;

import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;

/** 저장 결과 데이터 */
public record TeamPlayerSyncData(
	Long leagueId,
	String leagueApiId,
	String teamApiId,
	List<BetsSquadResponse.SquadPlayer> players
) {
}
