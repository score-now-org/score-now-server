package com.scorenow.scorenow_api.external.betsapi;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLeagueResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsTeamResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BetsApiClient {

	private final RestTemplate restTemplate;
	private final BetsApiProperties properties;

	/**
	 * 예정 경기 조회
	 */
	public BetsEventResponse getUpcomingEvents(String sportId, String day, int page) {
		String url = buildUrl("/v3/events/upcoming")
			.queryParam("sport_id", sportId)
			.queryParamIfPresent("day", java.util.Optional.ofNullable(day))
			.queryParam("page", page)
			.build().toUriString();

		log.debug("BetsAPI 호출: {}", maskToken(url));
		return restTemplate.getForObject(url, BetsEventResponse.class);
	}

	/**
	 * 진행 중 경기 조회
	 */
	public BetsEventResponse getInplayEvents(String sportId) {
		String url = buildUrl("/v3/events/inplay")
			.queryParam("sport_id", sportId)
			.build().toUriString();

		log.debug("BetsAPI 호출: {}", maskToken(url));
		return restTemplate.getForObject(url, BetsEventResponse.class);
	}

	/**
	 * 종료 경기 조회
	 */
	public BetsEventResponse getEndedEvents(String sportId, String day, int page) {
		String url = buildUrl("/v3/events/ended")
			.queryParam("sport_id", sportId)
			.queryParamIfPresent("day", java.util.Optional.ofNullable(day))
			.queryParam("page", page)
			.build().toUriString();

		log.debug("BetsAPI 호출: {}", maskToken(url));
		return restTemplate.getForObject(url, BetsEventResponse.class);
	}

	/**
	 * 리그 목록 조회
	 */
	public BetsLeagueResponse getLeagues(String sportId, int page) {
		String url = buildUrl("/v4/league")
			.queryParam("sport_id", sportId)
			.queryParam("page", page)
			.build().toUriString();

		log.debug("BetsAPI 호출: {}", maskToken(url));
		return restTemplate.getForObject(url, BetsLeagueResponse.class);
	}

	/**
	 * 팀 목록 조회
	 */
	public BetsTeamResponse getTeams(String sportId, String maxId) {
		String url = buildUrl("/v3/team")
			.queryParam("sport_id", sportId)
			.queryParamIfPresent("max_id", java.util.Optional.ofNullable(maxId))
			.build().toUriString();

		log.debug("BetsAPI 호출: {}", maskToken(url));
		return restTemplate.getForObject(url, BetsTeamResponse.class);
	}

	/**
	 * URL 빌더(공통)
	 */
	private UriComponentsBuilder buildUrl(String path) {
		return UriComponentsBuilder
			.fromHttpUrl(properties.getBaseUrl())
			.path(path)
			.queryParam("token", properties.getToken());
	}

	/**
	 * 로그에 토큰 노출 방지
	 */
	private String maskToken(String url) {
		return url.replaceAll("token=[^&]+", "token=***");
	}
}
