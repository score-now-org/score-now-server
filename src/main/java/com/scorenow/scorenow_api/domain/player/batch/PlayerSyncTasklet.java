package com.scorenow.scorenow_api.domain.player.batch;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.dto.response.LeaguePlayerSyncResult;
import com.scorenow.scorenow_api.domain.player.service.LeaguePlayerSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerSyncTasklet implements Tasklet {

	private final LeagueRepository leagueRepository;
	private final LeaguePlayerSyncService leaguePlayerSyncService;

	@Override
	public RepeatStatus execute(
		StepContribution contribution,
		ChunkContext chunkContext
	) {
		List<League> leagues = leagueRepository.findAll();

		int successLeagueCount = 0;
		int failedLeagueCount = 0;
		int totalProcessedPlayerCount = 0;

		for (League league : leagues) {
			try {
				LeaguePlayerSyncResult result = leaguePlayerSyncService.syncPlayersByLeague(league.getId());

				successLeagueCount++;
				totalProcessedPlayerCount += result.getProcessedPlayerCount();

				log.info(
					"리그 선수 동기화 결과. "
						+ "leagueId={}, "
						+ "leagueApiId={}, "
						+ "season={}, "
						+ "totalTeamCount={}, "
						+ "successTeamCount={}, "
						+ "failedTeamCount={}, "
						+ "failedTeamApiIds={}, "
						+ "processedPlayerCount={}",
					result.getLeagueId(),
					result.getLeagueApiId(),
					result.getSeasonName(),
					result.getTotalTeamCount(),
					result.getSuccessTeamCount(),
					result.getFailedTeamCount(),
					result.getFailedTeamApiIds(),
					result.getProcessedPlayerCount()
				);

			} catch (Exception e) {
				failedLeagueCount++;

				log.warn("리그 선수 동기화 실패. leagueId={}, cause={}",
					league.getId(), e.toString(), e);
			}
		}

		log.info(
			"전체 선수 동기화 배치 완료. totalLeagueCount={}, successLeagueCount={}, failedLeagueCount={}, totalProcessedPlayerCount={}",
			leagues.size(),
			successLeagueCount,
			failedLeagueCount,
			totalProcessedPlayerCount
		);

		return RepeatStatus.FINISHED;
	}
}
