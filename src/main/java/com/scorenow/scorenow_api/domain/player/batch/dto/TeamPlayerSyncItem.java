package com.scorenow.scorenow_api.domain.player.batch.dto;

/** TeamPlayerSyncItem
 * - 배치가 처리할 작업 단위 DTO
 * - Chunk에서 팀 1개를 처리하기 위한 DTO
 */
public record TeamPlayerSyncItem(
	Long leagueId,
	String leagueApiId,
	String teamApiId
) {

}
