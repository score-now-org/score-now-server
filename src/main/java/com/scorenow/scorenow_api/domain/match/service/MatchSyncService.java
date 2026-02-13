package com.scorenow.scorenow_api.domain.match.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.global.constant.AllowLeagues;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {

	private final BetsApiClient betsApiClient;
	private final MatchEventSyncService matchEventSyncService;

	/**
	 * 예정 경기 동기화
	 */
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
	public int syncInplayMatches(String sportId) {
		return syncEventsByLeagues("진행중", sportId, null,
			leagueId -> betsApiClient.getInplayEvents(sportId, leagueId));
	}

	/**
	 * 종료 경기 동기화
	 */
	public int syncEndedMatches(String sportId, String day) {
		final String normalizedDay = (day == null)
			? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
			: day;
		return syncEventsByLeagues("종료", sportId, normalizedDay,
			leagueId -> betsApiClient.getEndedEvents(sportId, leagueId, normalizedDay));
	}

	/**
	 * AllowLeagues 기준 리그별 이벤트를 가져와 동기화
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
						try {
							matchEventSyncService.syncEvent(event, sportId);
							totalSynced++;
						} catch (Exception e) {
							log.error("경기 동기화 실패 eventId: {} - {}", event.getId(), e.getMessage());
						}
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
}
