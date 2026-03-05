package com.scorenow.scorenow_api.domain.player.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.util.IdParser;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerSyncService {

	private final BetsApiClient betsApiClient;
	private final PlayerRepository playerRepo;
	private final LeagueRepository leagueRepo;

	@Transactional
	public int syncPlayersByLeague(String leagueId) {
		if (leagueId == null || leagueId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "leagueId가 비어있습니다.");
		}

		League league = leagueRepo.findById(leagueId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND, "리그를 찾을 수 없습니다: " + leagueId));

		String sportId = league.getSportId();
		String leagueApiId = IdParser.extractApiId(leagueId, sportId);

		BetsStandingsResponse standings = betsApiClient.getStandings(leagueApiId);
		if (standings == null || standings.getSuccess() == null || standings.getSuccess() != 1) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "팀순위(standings) 조회에 실패했습니다.");
		}

		String seasonName = extractSeasonName(standings);

		Set<String> teamApiIds = extractTeamApiIds(standings);
		if (teamApiIds.isEmpty()) {
			return 0;
		}

		int upsertCount = 0;

		for (String teamApiId : teamApiIds) {
			BetsSquadResponse squad = betsApiClient.getSquad(teamApiId);
			if (squad == null || squad.getSuccess() == null || squad.getSuccess() != 1 || squad.getResults() == null) {
				continue;
			}

			String teamId = sportId + teamApiId; // BETS1 + 17230 => BETS117230
			playerRepo.setSquadOffByTeamId(teamId);
			
			for (BetsSquadResponse.SquadPlayer sp : squad.getResults()) {
				if (sp == null || sp.getId() == null || sp.getId().isBlank())
					continue;

				Player p = new Player();
				p.setLeagueId(leagueId);
				p.setTeamId(teamId);
				p.setSeason(seasonName);

				// id: teamId:playerApiId
				p.setId(teamId + ":" + sp.getId());

				p.setEName(sp.getName());
				p.setCc(sp.getCc());
				p.setBirthdate(sp.getBirthdate());
				p.setPosition(sp.getPosition());
				p.setHeight(parseNullableInt(sp.getHeight()));
				p.setShirtnumber(sp.getShirtnumber());

				p.setSquadOn(true);

				playerRepo.save(p);
				upsertCount++;
			}
		}

		return upsertCount;

	}

	/** ========================================================== */

	private String extractSeasonName(BetsStandingsResponse standings) {
		List<BetsStandingsResponse.Result> results = standings.getResults();
		if (results == null || results.isEmpty())
			return null;

		BetsStandingsResponse.Result r = results.get(0);
		if (r == null || r.getSeason() == null)
			return null;

		return r.getSeason().getName();
	}

	private Set<String> extractTeamApiIds(BetsStandingsResponse standings) {
		Set<String> ids = new LinkedHashSet<>();
		List<BetsStandingsResponse.Result> results = standings.getResults();
		if (results == null)
			return ids;

		for (BetsStandingsResponse.Result r : results) {
			if (r == null || r.getOverall() == null || r.getOverall().getTables() == null)
				continue;

			for (BetsStandingsResponse.Table t : r.getOverall().getTables()) {
				if (t == null || t.getRows() == null)
					continue;

				for (BetsStandingsResponse.Row row : t.getRows()) {
					if (row == null || row.getTeam() == null)
						continue;

					String teamApiId = row.getTeam().getId();
					if (teamApiId != null && !teamApiId.isBlank())
						ids.add(teamApiId.trim());
				}
			}
		}

		return ids;
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
