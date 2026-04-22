package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.util.IdParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchLineupSyncService {

	private final MatchLineupMapper lineupMapper;

	private final BetsApiClient betsApiClient;
	private final MatchLineupRepository matchLineupRepo;

	private final MatchRepository matchRepo;
	private final TeamRepository teamRepo;

	/**
	 * 외부 Lineup API를 호출해 라인업 도큐먼트를 upsert
	 * - matchId에서 eventId 추출 후 lineup 조회 (matchId = sportId + eventId)
	 * - Match의 home/away teamId로 팀 eName 매핑
	 * - home/away 라인업을 문서에 세팅 후 저장
	 */
	@Transactional
	public MatchLineupDocument syncMatchLineup(String matchId, String sportId) {

		if (matchId == null || matchId.isBlank() || sportId == null || sportId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId/sportId가 비어있습니다.");
		}
		if (!matchId.startsWith(sportId)) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_INVALID_MATCH_SPORT,
				"matchId/sportId가 일치하지 않습니다.",
				"matchId=" + matchId + ", sportId=" + sportId
			);
		}

		Match match = matchRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_NOT_FOUND,
				"경기가 존재하지 않습니다.",
				matchId
			));

		String homeId = match.getHomeId();
		String awayId = match.getAwayId();

		Map<String, String> eNameMap = teamRepo.findAllById(List.of(homeId, awayId)).stream()
			.collect(Collectors.toMap(Team::getId, Team::getEName));

		String homeEname = eNameMap.get(homeId);
		String awayEname = eNameMap.get(awayId);

		if (homeEname == null || homeEname.isBlank()) {
			throw new BusinessException(
				ErrorCode.TEAM_NOT_FOUND,
				"홈팀 eName 누락",
				Map.of("homeId", homeId)
			);
		}
		if (awayEname == null || awayEname.isBlank()) {
			throw new BusinessException(
				ErrorCode.TEAM_NOT_FOUND,
				"원정팀 eName 누락",
				Map.of("awayId", awayId)
			);
		}

		// 라인업 API 호출
		String eventId = IdParser.extractEventId(matchId, sportId);
		BetsLineupResponse response = betsApiClient.getLineup(eventId);
		validateLineupResponse(response, eventId);

		BetsLineupResponse.Result results = response.getResults();

		// match_lineups doc에 upsert
		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseGet(() -> MatchLineupDocument.create(matchId));

		doc.setHome(lineupMapper.toSide(results.getHome(), homeId, homeEname));
		doc.setAway(lineupMapper.toSide(results.getAway(), awayId, awayEname));

		return matchLineupRepo.save(doc);
	}

	private void validateLineupResponse(BetsLineupResponse response, String eventId) {
		if (response == null) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 응답이 null입니다.",
				"eventId=" + eventId
			);
		}
		if (response.getSuccess() == null || response.getSuccess() != 1) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 호출 실패",
				"success=" + response.getSuccess() + ", eventId=" + eventId
			);
		}
		if (response.getResults() == null
			|| response.getResults().getHome() == null
			|| response.getResults().getAway() == null) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 결과가 비어있습니다.",
				"eventId=" + eventId
			);
		}
	}

	/** 라인업 도큐먼트 존재 여부(스케줄러 안전장치용) */
	@Transactional(readOnly = true)
	public boolean exists(String matchId) {
		if (matchId == null || matchId.isBlank())
			return false;
		return matchLineupRepo.existsById(matchId);
	}
}
