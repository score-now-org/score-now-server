package com.scorenow.scorenow_api.domain.match.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchEventSyncService {

	private static final String TEAM_IMAGE_BASE_URL = "https://assets.b365api.com/images/team/m/";
	private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final MatchRepository matchRepository;
	private final TeamRepository teamRepository;
	private final LeagueRepository leagueRepository;

	/**
	 * 개별 경기 동기화 - league → team → match 순서 보장
	 * REQUIRES_NEW: 경기 1건 실패가 다른 경기에 영향 없음
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void syncEvent(BetsEventResponse.Event event, String sportId) {
		saveLeague(event.getLeague(), sportId);
		saveTeam(event.getHome(), sportId);
		saveTeam(event.getAway(), sportId);
		saveMatch(event, sportId);
	}

	private void saveLeague(BetsEventResponse.League betsLeague, String sportId) {
		if (betsLeague == null) {
			return;
		}

		leagueRepository.insertIgnore(
			League.generateLeagueId(sportId, betsLeague.getId()),
			sportId,
			betsLeague.getName(),
			betsLeague.getName(),
			null,
			betsLeague.getCc()
		);
	}

	private void saveTeam(BetsEventResponse.Team betsTeam, String sportId) {
		if (betsTeam == null) {
			return;
		}

		String imageUrl = null;
		if (betsTeam.getImageId() != null) {
			imageUrl = TEAM_IMAGE_BASE_URL + betsTeam.getImageId() + ".png";
		}

		teamRepository.insertIgnore(
			Team.generateId(sportId, betsTeam.getId()),
			sportId,
			null,
			betsTeam.getName(),
			betsTeam.getName(),
			null,
			betsTeam.getCc(),
			imageUrl
		);
	}

	private void saveMatch(BetsEventResponse.Event event, String sportId) {
		if (event.getLeague() == null || event.getHome() == null || event.getAway() == null) {
			log.warn("경기 저장 스킵 - 필수 정보 누락 (league/home/away) eventId: {}", event.getId());
			return;
		}

		String matchId = Match.generateMatchId(sportId, event.getId());

		Match match = matchRepository.findById(matchId)
			.orElse(Match.builder().id(matchId).betsApiEventId(event.getId()).bet365Id(event.getBet365Id()).build());

		match.updateSportId(sportId);
		match.updateLeagueId(League.generateLeagueId(sportId, event.getLeague().getId()));
		match.updateHomeId(Team.generateId(sportId, event.getHome().getId()));
		match.updateAwayId(Team.generateId(sportId, event.getAway().getId()));
		match.updateStatus(MatchStatus.fromCode(event.getTimeStatus()));

		if (event.getTime() != null) {
			long timestamp = Long.parseLong(event.getTime());
			match.updateStartAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), DEFAULT_ZONE_ID));
		}

		if (event.getSs() != null && event.getSs().contains("-")) {
			String[] scores = event.getSs().split("-");
			if (scores.length == 2) {
				match.updateHomeScore(Integer.parseInt(scores[0].trim()));
				match.updateAwayScore(Integer.parseInt(scores[1].trim()));
			}
		}

		matchRepository.save(match);
		log.debug("경기 저장 - {}", matchId);
	}
}
