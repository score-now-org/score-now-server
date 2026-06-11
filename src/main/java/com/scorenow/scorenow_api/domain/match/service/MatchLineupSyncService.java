package com.scorenow.scorenow_api.domain.match.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

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
	private final TeamExternalMappingRepository teamExternalMappingRepository;
	private final PlayerExternalMappingRepository playerExternalMappingRepository;

	@Transactional
	public MatchLineupDocument syncMatchLineup(String matchIdValue, String sportIdValue) {

		Long matchId = parseRequiredLong(matchIdValue, "matchId");
		Long sportId = parseRequiredLong(sportIdValue, "sportId");

		Match match = matchRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_NOT_FOUND,
				"경기가 존재하지 않습니다.",
				"matchId=" + matchId
			));

		if (!sportId.equals(match.getSportId())) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_INVALID_MATCH_SPORT,
				"matchId/sportId가 일치하지 않습니다.",
				"matchId=" + matchId + ", sportId=" + sportId
			);
		}

		if (match.getApiMatchId() == null || match.getApiMatchId().isBlank()) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"apiMatchId가 비어있습니다.",
				"matchId=" + matchId
			);
		}

		Long homeId = match.getHomeId();
		Long awayId = match.getAwayId();

		Map<Long, Team> teamMap = teamRepo.findAllById(List.of(homeId, awayId)).stream()
			.collect(Collectors.toMap(Team::getId, team -> team));

		Team homeTeam = teamMap.get(homeId);
		Team awayTeam = teamMap.get(awayId);

		if (homeTeam == null || homeTeam.getEName() == null || homeTeam.getEName().isBlank()) {
			throw new BusinessException(
				ErrorCode.TEAM_NOT_FOUND,
				"홈팀 eName 누락",
				Map.of("homeId", homeId)
			);
		}

		if (awayTeam == null || awayTeam.getEName() == null || awayTeam.getEName().isBlank()) {
			throw new BusinessException(
				ErrorCode.TEAM_NOT_FOUND,
				"원정팀 eName 누락",
				Map.of("awayId", awayId)
			);
		}

		String apiHomeId = findApiTeamId(homeId);
		String apiAwayId = findApiTeamId(awayId);
		String apiMatchId = match.getApiMatchId();

		BetsLineupResponse lineupResponse = betsApiClient.getLineup(apiMatchId);
		validateLineupResponse(lineupResponse, apiMatchId);

		Map<String, Long> playerIdMap = resolvePlayerIdMap(lineupResponse);

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseGet(() -> MatchLineupDocument.create(
				matchId,
				sportId,
				match.getApiMatchId()
			));

		doc.setHome(lineupMapper.toSide(
			lineupResponse.getResults().getHome(),
			homeId,
			apiHomeId,
			homeTeam.getEName(),
			playerIdMap
		));

		doc.setAway(lineupMapper.toSide(
			lineupResponse.getResults().getAway(),
			awayId,
			apiAwayId,
			awayTeam.getEName(),
			playerIdMap
		));

		return matchLineupRepo.save(doc);
	}

	private Map<String, Long> resolvePlayerIdMap(BetsLineupResponse lineupResponse) {
		Set<String> apiPlayerIds = extractApiPlayerIds(lineupResponse);

		if (apiPlayerIds.isEmpty()) {
			return Map.of();
		}

		return playerExternalMappingRepository
			.findByProviderAndApiPlayerIdIn(DataOrigin.BETS, apiPlayerIds)
			.stream()
			.filter(mapping -> mapping.getPlayer() != null)
			.collect(Collectors.toMap(
				PlayerExternalMapping::getApiPlayerId,
				mapping -> mapping.getPlayer().getId(),
				(existing, replacement) -> existing
			));
	}

	private Set<String> extractApiPlayerIds(BetsLineupResponse lineupResponse) {
		Set<String> apiPlayerIds = new HashSet<>();

		if (lineupResponse == null || lineupResponse.getResults() == null) {
			return apiPlayerIds;
		}

		collectApiPlayerIds(lineupResponse.getResults().getHome(), apiPlayerIds);
		collectApiPlayerIds(lineupResponse.getResults().getAway(), apiPlayerIds);

		return apiPlayerIds;
	}

	private void collectApiPlayerIds(
		BetsLineupResponse.LineupSide side,
		Set<String> apiPlayerIds
	) {
		if (side == null) {
			return;
		}

		addApiPlayerIds(side.getStartinglineup(), apiPlayerIds);
		addApiPlayerIds(side.getSubstitutes(), apiPlayerIds);
	}

	private void addApiPlayerIds(
		List<BetsLineupResponse.LineupPlayer> players,
		Set<String> apiPlayerIds
	) {
		if (players == null) {
			return;
		}

		for (BetsLineupResponse.LineupPlayer player : players) {
			if (player == null || player.getPlayer() == null) {
				continue;
			}

			String apiPlayerId = player.getPlayer().getId();

			if (apiPlayerId != null && !apiPlayerId.isBlank()) {
				apiPlayerIds.add(apiPlayerId);
			}
		}
	}

	private String findApiTeamId(Long teamId) {
		return teamExternalMappingRepository
			.findByProviderAndInternalTeamId(DataOrigin.BETS, teamId) // 메서드명 수정
			.map(TeamExternalMapping::getApiTeamId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"팀 외부 API 매핑 정보를 찾을 수 없습니다.",
				"teamId=" + teamId
			));
	}

	private Long parseRequiredLong(String raw, String fieldName) {
		if (raw == null || raw.isBlank()) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				fieldName + "가 비어있습니다."
			);
		}

		try {
			return Long.valueOf(raw);
		} catch (NumberFormatException e) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				fieldName + "는 숫자 형식이어야 합니다.",
				fieldName + "=" + raw
			);
		}
	}

	private void validateLineupResponse(BetsLineupResponse response, String apiMatchId) {
		if (response == null) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 응답이 null입니다.",
				"apiMatchId=" + apiMatchId
			);
		}

		if (response.getSuccess() == null || response.getSuccess() != 1) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 호출 실패",
				"success=" + response.getSuccess() + ", apiMatchId=" + apiMatchId
			);
		}

		if (response.getResults() == null) {
			throw new BusinessException(
				ErrorCode.INTERNAL_SERVER_ERROR,
				"라인업 API 결과가 비어있습니다.",
				"apiMatchId=" + apiMatchId
			);
		}
	}

	@Transactional(readOnly = true)
	public boolean exists(String matchIdValue) {
		if (matchIdValue == null || matchIdValue.isBlank()) {
			return false;
		}

		try {
			return matchLineupRepo.existsById(Long.valueOf(matchIdValue));
		} catch (NumberFormatException e) {
			return false;
		}
	}
}