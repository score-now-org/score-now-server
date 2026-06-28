package com.scorenow.scorenow_api.domain.player.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.player.batch.dto.TeamPlayerSyncData;
import com.scorenow.scorenow_api.domain.player.batch.dto.TeamPlayerSyncItem;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Squad API 호출 및 저장 데이터 변환
 * Reader가 만든 TeamPlayerSyncItem을 받아서 squad API를 호출하는 역할
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamPlayerSyncProcessor implements ItemProcessor<TeamPlayerSyncItem, TeamPlayerSyncData> {

	private final BetsApiClient betsApiClient;

	@Override
	public TeamPlayerSyncData process(TeamPlayerSyncItem item) { // Reader가 넘긴 item 하나를 처리

		// teamApiId로 squad API를 호출
		BetsSquadResponse squad = betsApiClient.getSquad(item.teamApiId());

		if (!isValidSquad(squad)) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"스쿼드 응답이 유효하지 않습니다. teamApiId=" + item.teamApiId()
			);
		}

		if (squad.getResults().isEmpty()) {
			// throw new BusinessException(
			// 	ErrorCode.INTERNAL_SERVER_ERROR,
			// 	"스쿼드 결과가 비어있습니다. teamApiId=" + item.teamApiId()
			// );

			log.warn(
				"스쿼드 결과가 비어있습니다. 선수 저장을 건너뜁니다. leagueId={}, leagueApiId={}, teamApiId={}",
				item.leagueId(),
				item.leagueApiId(),
				item.teamApiId()
			);
		}

		// Writer에게 넘길 TeamPlayerSyncData 생성
		return new TeamPlayerSyncData(
			item.leagueId(),
			item.leagueApiId(),
			item.teamApiId(),
			squad.getResults()
		);
	}

	private boolean isValidSquad(BetsSquadResponse squad) {
		if (squad != null
			&& squad.getSuccess() != null
			&& squad.getSuccess() == 1
			&& squad.getResults() != null) {

			return true;
		}

		return false;
	}
}
