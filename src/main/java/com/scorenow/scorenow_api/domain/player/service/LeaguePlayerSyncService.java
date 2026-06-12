package com.scorenow.scorenow_api.domain.player.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.dto.response.LeaguePlayerSyncResult;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaguePlayerSyncService {

	private final BetsApiClient betsApiClient;
	private final LeagueRepository leagueRepository;
	private final LeagueExternalMappingRepository leagueExternalMappingRepository;
	private final TeamPlayerSyncService teamPlayerSyncService;

	public LeaguePlayerSyncResult syncPlayersByLeague(Long leagueId) {

		if (leagueId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "leagueId가 비어있습니다.");
		}

		League league = leagueRepository.findById(leagueId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.LEAGUE_NOT_FOUND,
				"리그를 찾을 수 없습니다: " + leagueId
			));

		String leagueApiId = leagueExternalMappingRepository
			.findByProviderAndInternalLeagueId(DataOrigin.BETS, league.getId())
			.map(LeagueExternalMapping::getApiLeagueId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"리그 매핑 정보를 찾을 수 없습니다. leagueId=" + leagueId
			));

		BetsStandingsResponse standings = betsApiClient.getStandings(leagueApiId);

		if (standings == null || standings.getSuccess() == null || standings.getSuccess() != 1) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "팀순위(standings) 조회에 실패했습니다.");
		}

		String seasonName = extractSeasonName(standings);

		Set<String> teamApiIds = extractTeamApiIds(standings);

		LeaguePlayerSyncResult result = new LeaguePlayerSyncResult(
			league.getId(),
			leagueApiId,
			seasonName,
			teamApiIds.size()
		);

		if (teamApiIds.isEmpty()) {
			return result;
		}

		for (String teamApiId : teamApiIds) {
			try {
				int processedCount = teamPlayerSyncService.syncTeamPlayers(
					league.getId(),
					seasonName,
					teamApiId
				);

				result.addSuccessTeam(processedCount);

			} catch (Exception e) {
				result.addFailedTeam(teamApiId);

				log.warn("팀 선수 동기화 실패. leagueId={}, leagueApiId={}, teamApiId={}, cause={}",
					leagueId, leagueApiId, teamApiId, e.toString(), e);
			}
		}

		return result;
	}

	private String extractSeasonName(BetsStandingsResponse standings) {
		List<BetsStandingsResponse.Result> results = standings.getResults();
		if (results == null || results.isEmpty()) {
			return null;
		}

		BetsStandingsResponse.Result r = results.get(0);
		if (r == null || r.getSeason() == null) {
			return null;
		}

		return r.getSeason().getName();
	}

	private Set<String> extractTeamApiIds(BetsStandingsResponse standings) {
		Set<String> ids = new LinkedHashSet<>();
		List<BetsStandingsResponse.Result> results = standings.getResults();
		if (results == null) {
			return ids;
		}

		for (BetsStandingsResponse.Result r : results) {
			if (r == null || r.getOverall() == null || r.getOverall().getTables() == null) {
				continue;
			}

			for (BetsStandingsResponse.Table t : r.getOverall().getTables()) {
				if (t == null || t.getRows() == null) {
					continue;
				}

				for (BetsStandingsResponse.Row row : t.getRows()) {
					if (row == null || row.getTeam() == null) {
						continue;
					}

					String teamApiId = row.getTeam().getId();
					if (teamApiId != null && !teamApiId.isBlank()) {
						ids.add(teamApiId.trim());
					}
				}
			}
		}

		return ids;
	}
}