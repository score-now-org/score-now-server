package com.scorenow.scorenow_api.domain.player.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.external.common.ExternalProvider;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamPlayerSyncService {

	private final BetsApiClient betsApiClient;

	private final PlayerRepository playerRepository;
	private final PlayerExternalMappingRepository playerExternalMappingRepository;
	private final PlayerTeamDetailRepository playerTeamDetailRepository;

	private final TeamExternalMappingRepository teamExternalMappingRepository;
	private final LeagueRepository leagueRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int syncTeamPlayers(Long leagueId, String seasonName, String teamApiId) {

		Team team = teamExternalMappingRepository
			.findByProviderAndApiTeamId(ExternalProvider.BETS, teamApiId)
			.map(TeamExternalMapping::getExternalTeamId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"팀 매핑 정보를 찾을 수 없습니다. teamApiId=" + teamApiId
			));

		League league = leagueRepository.findById(leagueId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"리그 정보를 찾을 수 없습니다. leagueId=" + leagueId
			));

		BetsSquadResponse squad = betsApiClient.getSquad(teamApiId);

		if (!isValidSquad(squad)) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"스쿼드 응답이 유효하지 않습니다. teamApiId=" + teamApiId
			);
		}

		if (squad.getResults().isEmpty()) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"스쿼드 결과가 비어있습니다. teamApiId=" + teamApiId
			);
		}

		playerTeamDetailRepository.setSquadOffByTeamIdAndLeagueIdAndSeason(
			team.getId(),
			league.getId(),
			seasonName
		);

		int savedCount = 0;

		for (BetsSquadResponse.SquadPlayer sp : squad.getResults()) {
			if (sp == null || sp.getId() == null || sp.getId().isBlank()) {
				continue;
			}

			String apiPlayerId = normalizeApiId(sp.getId());
			if (apiPlayerId == null) {
				continue;
			}

			Player player = findOrCreatePlayer(sp, apiPlayerId);

			PlayerTeamDetail detail = playerTeamDetailRepository
				.findByPlayerIdAndTeamIdAndLeagueIdAndSeason(
					player.getId(),
					team.getId(),
					league.getId(),
					seasonName
				)
				.orElseGet(() -> PlayerTeamDetail.builder()
					.player(player)
					.team(team)
					.league(league)
					.season(seasonName)
					.build());

			detail.setPosition(sp.getPosition());
			detail.setShirtNumber(sp.getShirtnumber());
			detail.setSquadOn(true);

			playerTeamDetailRepository.save(detail);
			savedCount++;
		}

		return savedCount;
	}

	private Player findOrCreatePlayer(BetsSquadResponse.SquadPlayer sp, String apiPlayerId) {
		return playerExternalMappingRepository
			.findByProviderAndApiPlayerId(ExternalProvider.BETS, apiPlayerId)
			.map(PlayerExternalMapping::getPlayer)
			.orElseGet(() -> {
				Player player = Player.builder()
					.eName(sp.getName())
					.kName(null)
					.cc(sp.getCc())
					.birthdate(sp.getBirthdate())
					.height(parseNullableInt(sp.getHeight()))
					.teaguk(false)
					.build();

				Player savedPlayer = playerRepository.save(player);

				PlayerExternalMapping mapping = new PlayerExternalMapping(
					ExternalProvider.BETS,
					apiPlayerId,
					savedPlayer
				);

				playerExternalMappingRepository.save(mapping);

				return savedPlayer;
			});
	}

	private boolean isValidSquad(BetsSquadResponse squad) {
		return squad != null
			&& squad.getSuccess() != null
			&& squad.getSuccess() == 1
			&& squad.getResults() != null;
	}

	private Integer parseNullableInt(String raw) {
		if (raw == null) {
			return null;
		}

		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}

		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String normalizeApiId(String raw) {
		if (raw == null) {
			return null;
		}

		String s = raw.trim();
		return s.isEmpty() ? null : s;
	}

}