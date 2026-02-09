package com.scorenow.scorenow_api.domain.match.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.global.constant.AllowLeagues;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {

	private static final String TEAM_IMAGE_BASE_URL = "https://assets.b365api.com/images/team/m/";
	private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final BetsApiClient betsApiClient;
	private final MatchRepository matchRepository;
	private final LeagueRepository leagueRepository;
	private final TeamRepository teamRepository;

	/**
	 * 예정 경기 동기화
	 */
	@Transactional
	public int syncUpcomingMatches(String sportId, String day) {
		final String normalizedDay = (day == null) 
			? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
			: day;
		return syncEventsByLeagues("예정", sportId, normalizedDay,
			leagueId -> betsApiClient.getUpcomingEvents(sportId, leagueId, normalizedDay));
	}

	/**
	 * 진행 중 경기 동기화
	 */
	@Transactional
	public int syncInplayMatches(String sportId) {
		return syncEventsByLeagues("진행중", sportId, null,
			leagueId -> betsApiClient.getInplayEvents(sportId, leagueId));
	}

	/**
	 * 종료 경기 동기화
	 */
	@Transactional
	public int syncEndedMatches(String sportId, String day) {
		final String normalizedDay = (day == null)
			? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
			: day;
		return syncEventsByLeagues("종료", sportId, normalizedDay,
			leagueId -> betsApiClient.getEndedEvents(sportId, leagueId, normalizedDay));
	}

	/**
	 * AllowLeagues 기준 리그별 이벤트를 가져와 동기화
	 * fetchEvents: leagueId만 받아서 BetsEventResponse 반환 (sportId, day는 호출부에서 람다로 캡처)
	 */
	private int syncEventsByLeagues(String logLabel, String sportId, String day,
			Function<String, BetsEventResponse> fetchEvents) {
		if (day != null) {
			log.info("{} 경기 동기화 시작 - sportId: {}, day: {}", logLabel, sportId, day);
		} else {
			log.info("{} 경기 동기화 시작 - sportId: {}", logLabel, sportId);
		}

		int totalSynced = 0;
		for (String leagueId : AllowLeagues.IDS) {
			try {
				BetsEventResponse response = fetchEvents.apply(leagueId);
				if (response != null && response.getResults() != null) {
					for (BetsEventResponse.Event event : response.getResults()) {
						syncEvent(event, sportId);
						totalSynced++;
					}
					if (!response.getResults().isEmpty()) {
						log.debug("리그 {} 동기화: {}건", leagueId, response.getResults().size());
					}
				}
			} catch (Exception e) {
				log.error("리그 {} 동기화 실패: {}", leagueId, e.getMessage());
			}
		}

		log.info("{} 경기 동기화 완료 - 총 {}건", logLabel, totalSynced);
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
				imageUrl = TEAM_IMAGE_BASE_URL + betsTeam.getImageId() + ".png";
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
		if (event.getLeague() == null || event.getHome() == null || event.getAway() == null) {
			log.warn("경기 저장 스킵 - 필수 정보 누락 (league/home/away) eventId: {}", event.getId());
			return;
		}

		String matchId = Match.generateMatchId(sportId, event.getId());

		Match match = matchRepository.findById(matchId)
			.orElse(Match.builder().id(matchId).betsApiEventId(event.getId()).bet365Id(event.getBet365Id()).build());

		// 기본 정보 업데이트
		match.updateSportId(sportId);
		match.updateLeagueId(League.generateLeagueId(sportId, event.getLeague().getId()));
		match.updateHomeId(Team.generateId(sportId, event.getHome().getId()));
		match.updateAwayId(Team.generateId(sportId, event.getAway().getId()));
		match.updateStatus(MatchStatus.fromCode(event.getTimeStatus()));

		// 시작 시간
		if (event.getTime() != null) {
			long timestamp = Long.parseLong(event.getTime());
			match.updateStartAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), DEFAULT_ZONE_ID));
		}

		// 스코어
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
