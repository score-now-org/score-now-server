package com.scorenow.scorenow_api.domain.match.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {

	private final BetsApiClient betsApiClient;
	private final MatchRepository matchRepository;
	private final LeagueRepository leagueRepository;
	private final TeamRepository teamRepository;

	/**
	 * 예정 경기 동기화
	 */
	@Transactional
	public int syncUpcomingMatches(String sportId, String day) {
		if (day == null) {
			day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		}

		log.info("예정 경기 동기화 시작 - sportsId: {}, day: {}", sportId, day);

		int totalSynced = 0;
		int page = 1;

		while (page <= 10) {
			BetsEventResponse response = betsApiClient.getUpcomingEvents(sportId, day, page);

			if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
				break;
			}

			for (BetsEventResponse.Event event : response.getResults()) {
				try {
					syncEvent(event, sportId);
					totalSynced++;
				} catch (Exception e) {
					log.error("경기 동기화 실패 - eventId: {}, error: {}", event.getId(), e.getMessage());
				}
			}

			// 다음 페이지 있는지 확인
			if (response.getResults().size() < 50) {
				break;
			}
			page++;
		}

		log.info("예정 경기 동기화 완료 - 총 {}건", totalSynced);
		return totalSynced;
	}

	/**
	 * 진행 중 경기 동기화
	 */
	@Transactional
	public int syncInplayMatches(String sportId) {
		log.info("진행 중 경기 동기화 시작 - sportId: {}", sportId);

		BetsEventResponse response = betsApiClient.getInplayEvents(sportId);

		if (response == null || response.getResults() == null) {
			return 0;
		}

		int totalSynced = 0;
		for (BetsEventResponse.Event event : response.getResults()) {
			try {
				syncEvent(event, sportId);
				totalSynced++;
			} catch (Exception e) {
				log.error("경기 동기화 실패 - eventId: {}, error: {}", event.getId(), e.getMessage());
			}
		}

		log.info("진행 중 경기 동기화 완료 - 총 {}건", totalSynced);
		return totalSynced;
	}

	/**
	 * 종료 경기 동기화
	 */
	@Transactional
	public int syncEndedMatches(String sportId, String day) {
		if (day == null) {
			day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		}

		log.info("종료 경기 동기화 시작 - sportId: {}, day: {}", sportId, day);

		int totalSynced = 0;
		int page = 1;

		while (page <= 10) {
			BetsEventResponse response = betsApiClient.getEndedEvents(sportId, day, page);

			if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
				break;
			}

			for (BetsEventResponse.Event event : response.getResults()) {
				try {
					syncEvent(event, sportId);
					totalSynced++;
				} catch (Exception e) {
					log.error("경기 동기화 실패 - eventId: {}, error: {}", event.getId(), e.getMessage());
				}
			}

			if (response.getResults().size() < 50) {
				break;
			}
			page++;
		}

		log.info("종료 경기 동기화 완료 - 총 {}건", totalSynced);
		return totalSynced;
	}

	/**
	 * 개별 경기 동기화
	 */
	private void syncEvent(BetsEventResponse.Event event, String sportId) {
		// 1. 리그 저장
		saveLeague(event.getLeague(), sportId);

		// 2. 팀 저장
		saveTeam(event.getHome(), sportId);
		saveTeam(event.getAway(), sportId);

		// 3. 경기 저장
		saveMatch(event, sportId);
	}

	private void saveLeague(BetsEventResponse.League betsLeague, String sportId) {
		if (betsLeague == null)
			return;

		String leagueId = League.generateLeagueId(sportId, betsLeague.getId());

		if (!leagueRepository.existsById(leagueId)) {
			League league = League.builder()
				.id(leagueId)
				.sportId(sportId)
				.eName(betsLeague.getName())
				.kName(betsLeague.getName())
				.cc(betsLeague.getCc())
				.build();

			leagueRepository.save(league);
			log.debug("리그 저장 - {}", leagueId);
		}
	}

	private void saveTeam(BetsEventResponse.Team betsTeam, String sportId) {
		if (betsTeam == null)
			return;

		String teamId = Team.generateId(sportId, betsTeam.getId());

		if (!teamRepository.existsById(teamId)) {
			String imageUrl = null;
			if (betsTeam.getImageId() != null) {
				imageUrl = "https://assets.b365api.com/images/team/m/" + betsTeam.getImageId() + ".png";
			}

			Team team = Team.builder()
				.id(teamId)
				.sportId(sportId)
				.eName(betsTeam.getName())
				.kName(betsTeam.getName())
				.cc(betsTeam.getCc())
				.imageUrl(imageUrl)
				.build();
			teamRepository.save(team);
			log.debug("팀 저장 - {}", teamId);
		}
	}

	private void saveMatch(BetsEventResponse.Event event, String sportId) {
		String matchId = Match.generateMatchId(sportId, event.getId());

		Match match = matchRepository.findById(matchId)
			.orElse(Match.builder().id(matchId).betsApiEventId(event.getId()).bet365Id(event.getBet365Id()).build());

		// 기본 정보 업데이트
		match.setSportId(sportId);
		match.setLeagueId(League.generateLeagueId(sportId, event.getLeague().getId()));
		match.setHomeId(Team.generateId(sportId, event.getHome().getId()));
		match.setAwayId(Team.generateId(sportId, event.getAway().getId()));
		match.setStatusCode(MatchStatus.fromCode(event.getTimeStatus()));

		// 시작 시간
		if (event.getTime() != null) {
			long timestamp = Long.parseLong(event.getTime());
			match.setStartAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("Asia/Seoul")));
		}

		// 스코어
		if (event.getSs() != null && event.getSs().contains("-")) {
			String[] scores = event.getSs().split("-");
			if (scores.length == 2) {
				match.setHomeScore(Integer.parseInt(scores[0].trim()));
				match.setAwayScore(Integer.parseInt(scores[1].trim()));
			}
		}

		matchRepository.save(match);
		log.debug("경기 저장 - {}", matchId);
	}

}
