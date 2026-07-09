package com.scorenow.scorenow_api.domain.player.batch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingJpaRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.batch.dto.PlayerSyncItem;
import com.scorenow.scorenow_api.domain.team.service.TeamService;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 동기화 대상 팀 생성
 */

@Component
@StepScope
@RequiredArgsConstructor
public class PlayerSyncItemReader implements ItemReader<PlayerSyncItem> {

	private final LeagueRepository leagueRepository;
	private final LeagueExternalMappingJpaRepository leagueExternalMappingJpaRepository;
	private final BetsApiClient betsApiClient;
	private final TeamService teamService;

	private List<PlayerSyncItem> items;
	private int currentIndex = 0;

	@Override
	public PlayerSyncItem read() {

		if (items == null) {
			items = createItems();
		}

		if (currentIndex >= items.size()) {
			return null;
		}

		return items.get(currentIndex++);
	}

	private List<PlayerSyncItem> createItems() {

		List<PlayerSyncItem> items = new ArrayList<>();
		// List<League> leagues = leagueRepository.findAll(); // DB에서 저장된 리그 조회
		List<LeagueExternalMapping> mappings = leagueExternalMappingJpaRepository.findByDataOrigin(DataOrigin.BETS);

		// 내부 리그 id로 외부 API 리그 id 조회
		// for (League league : leagues) {
		// 	String leagueApiId = leagueExternalMappingJpaRepository
		// 		.findByDataOriginAndInternalLeagueId(DataOrigin.BETS, league.getId())
		// 		.map(LeagueExternalMapping::getApiLeagueId)
		// 		.orElseThrow(() -> new BusinessException(
		// 			ErrorCode.INTERNAL_SERVER_ERROR,
		// 			"리그 매핑 정보를 찾을 수 없습니다. leagueId=" + league.getId()
		// 		));

		for (LeagueExternalMapping mapping : mappings) {

			String leagueApiId = mapping.getApiLeagueId();

			if (leagueApiId == null || leagueApiId.isBlank()) {
				continue;
			}

			leagueApiId = leagueApiId.trim();

			League league = leagueRepository.findById(mapping.getInternalLeagueId())
				.orElseThrow(() -> new BusinessException(
					ErrorCode.INTERNAL_SERVER_ERROR,
					"리그 정보를 찾을 수 없습니다. leagueId=" + mapping.getInternalLeagueId()
				));

			// BETS의 'league/table' API를 호출
			BetsStandingsResponse standings = betsApiClient.getStandings(leagueApiId);

			if (standings == null || standings.getSuccess() == null || standings.getSuccess() != 1) {
				throw new BusinessException(
					ErrorCode.INTERNAL_SERVER_ERROR,
					"팀순위(standings) 조회에 실패했습니다. leagueId=" + league.getId()
				);
			}

			// extractTeams() 으로 팀만 뽑아냄
			Set<BetsStandingsResponse.Team> teams = extractTeams(standings);

			for (BetsStandingsResponse.Team team : teams) {
				String imageUrl = teamService.buildImageUrl(team.getImageId());

				teamService.getOrCreateTeam(
					DataOrigin.BETS,
					league.getSportId(),
					team.getId(),
					team.getName(),
					team.getCc(),
					imageUrl
				);

				items.add(new PlayerSyncItem(
					league.getSportId(),
					league.getId(),
					leagueApiId,
					team.getId()
				));
			}
		}

		return items;
	}

	/** extractTeams */
	private Set<BetsStandingsResponse.Team> extractTeams(BetsStandingsResponse standings) {
		Set<BetsStandingsResponse.Team> teams = new LinkedHashSet<>();

		List<BetsStandingsResponse.Result> results = standings.getResults();
		if (results == null) {
			return teams;
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

					BetsStandingsResponse.Team team = row.getTeam();

					if (team.getId() != null && !team.getId().isBlank()) {
						team.setId(team.getId().trim());
						teams.add(team);
					}
				}
			}
		}

		return teams;
	}
}