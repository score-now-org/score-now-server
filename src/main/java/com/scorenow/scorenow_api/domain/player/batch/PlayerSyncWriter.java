package com.scorenow.scorenow_api.domain.player.batch;

import java.util.Optional;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.batch.dto.PlayerSyncData;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선수 저장
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerSyncWriter implements ItemWriter<PlayerSyncData> {

	private final PlayerRepository playerRepository;
	private final PlayerExternalMappingRepository playerExternalMappingRepository;
	private final PlayerTeamDetailRepository playerTeamDetailRepository;

	private final TeamExternalMappingRepository teamExternalMappingRepository;
	private final TeamRepository teamRepository;
	private final LeagueRepository leagueRepository;
	private final SportRepository sportRepository;

	// Writer는 Spring Batch 5 기준으로 List<> 대신 Chunk<>를 받아야 한다.
	// Spring Batch 4: write(List<? extends T> items)
	// Spring Batch 5: write(Chunk<? extends T> chunk)
	@Override
	public void write(Chunk<? extends PlayerSyncData> chunk) {
		for (PlayerSyncData data : chunk) {

			int savedCount = saveTeamPlayers(data);
			log.info(
				"팀 선수 동기화 저장 완료. sportId={}, leagueId={}, leagueApiId={}, teamApiId={}, savedCount={}",
				data.sportId(),
				data.leagueId(),
				data.leagueApiId(),
				data.teamApiId(),
				savedCount
			);
		}
	}

	private int saveTeamPlayers(PlayerSyncData data) {
		Sport sport = getSport(data.sportId());
		Team team = getTeam(data.teamApiId());
		League league = getLeague(data.leagueId());

		int savedCount = 0;

		for (BetsSquadResponse.SquadPlayer sp : data.players()) {

			String apiPlayerId = normalizeApiId(sp == null ? null : sp.getId());

			if (apiPlayerId == null) {
				continue;
			}

			Player player = upsertPlayer(sp, apiPlayerId, sport);
			upsertPlayerTeamDetail(sp, player, team, league);

			savedCount++;
		}

		return savedCount;
	}

	private Sport getSport(Long sportId) {
		return sportRepository.findById(sportId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"sportId에 해당하는 종목을 찾을 수 없습니다. sportId=" + sportId
			));
	}

	private Team getTeam(String teamApiId) {
		TeamExternalMapping teamMapping = teamExternalMappingRepository
			.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"teamApiId에 해당하는 팀을 찾을 수 없습니다. teamApiId=" + teamApiId
			));

		return teamRepository.findById(teamMapping.getInternalTeamId())
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"internalTeamId에 해당하는 팀을 찾을 수 없습니다. internalTeamId=" + teamMapping.getInternalTeamId()
			));
	}

	private League getLeague(Long leagueId) {
		return leagueRepository.findById(leagueId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"leagueId에 해당하는 리그를 찾을 수 없습니다. leagueId=" + leagueId
			));
	}

	private Player upsertPlayer(BetsSquadResponse.SquadPlayer sp, String apiPlayerId, Sport sport) {
		Optional<PlayerExternalMapping> optionalMapping = playerExternalMappingRepository
			.findByProviderAndApiPlayerId(DataOrigin.BETS, apiPlayerId);

		if (optionalMapping.isPresent()) {
			PlayerExternalMapping mapping = optionalMapping.get();
			Player player = mapping.getPlayer();

			if (player == null) {
				Player savedPlayer = playerRepository.save(newPlayer(sp, sport));

				mapping.changePlayer(savedPlayer);
				playerExternalMappingRepository.save(mapping);

				return savedPlayer;
			}

			player.updateFromApi(
				sp.getName(),
				sp.getCc(),
				sp.getBirthdate(),
				parseNullableInt(sp.getHeight())
			);

			return player;
		}

		Player savedPlayer = playerRepository.save(newPlayer(sp, sport));

		PlayerExternalMapping mapping = new PlayerExternalMapping(
			DataOrigin.BETS,
			apiPlayerId,
			savedPlayer
		);

		playerExternalMappingRepository.save(mapping);

		return savedPlayer;
	}

	private void upsertPlayerTeamDetail(
		BetsSquadResponse.SquadPlayer sp,
		Player player,
		Team team,
		League league
	) {
		PlayerTeamDetail detail = playerTeamDetailRepository
			.findByPlayerIdAndTeamIdAndLeagueId(
				player.getId(),
				team.getId(),
				league.getId()
			)
			.orElseGet(() -> PlayerTeamDetail.builder()
				.player(player)
				.team(team)
				.league(league)
				.build());

		detail.setPosition(sp.getPosition());
		detail.setShirtNumber(sp.getShirtnumber());

		playerTeamDetailRepository.save(detail);
	}

	private Player newPlayer(BetsSquadResponse.SquadPlayer sp, Sport sport) {
		return Player.builder()
			.sport(sport)
			.eName(sp.getName())
			.kName(null)
			.cc(sp.getCc())
			.birthdate(sp.getBirthdate())
			.height(parseNullableInt(sp.getHeight()))
			.teaguk(false)
			.build();
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
