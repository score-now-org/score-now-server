package com.scorenow.scorenow_api.domain.player.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamPlayerSyncService {

	private final BetsApiClient betsApiClient;
	private final PlayerRepository playerRepo;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int syncTeamPlayers(String leagueId, String sportId, String seasonName, String teamApiId) {

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

		String teamId = sportId + teamApiId; // BETS1 + 17230 => BETS117230
		playerRepo.setSquadOffByTeamId(teamId);

		List<Player> toSave = new ArrayList<>(squad.getResults().size());
		for (BetsSquadResponse.SquadPlayer sp : squad.getResults()) {
			if (sp == null || sp.getId() == null || sp.getId().isBlank())
				continue;

			Player p = Player.builder()
				.id(teamId + ":" + sp.getId())
				.leagueId(leagueId)
				.teamId(teamId)
				.season(seasonName)
				.eName(sp.getName())
				.cc(sp.getCc())
				.birthdate(sp.getBirthdate())
				.position(sp.getPosition())
				.height(parseNullableInt(sp.getHeight()))
				.shirtnumber(sp.getShirtnumber())
				.squadOn(true)
				.build();

			toSave.add(p);
		}

		playerRepo.saveAll(toSave);
		return toSave.size();

	}

	private boolean isValidSquad(BetsSquadResponse squad) {
		return squad != null
			&& squad.getSuccess() != null
			&& squad.getSuccess() == 1
			&& squad.getResults() != null;
	}

	private Integer parseNullableInt(String raw) {
		if (raw == null)
			return null;

		String s = raw.trim();
		if (s.isEmpty())
			return null;

		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}

	}
}
