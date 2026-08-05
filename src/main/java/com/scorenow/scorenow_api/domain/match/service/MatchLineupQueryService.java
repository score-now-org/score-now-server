package com.scorenow.scorenow_api.domain.match.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchLineupSearchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchLineupQueryService {

	private final MatchLineupSearchRepository matchLineupSearchRepository;
	private final MatchLineupRepository matchLineupRepo;

	/**
	 * matchId 기준으로 라인업 조회
	 */
	@Transactional(readOnly = true)
	public MatchLineupDocument getByMatchId(Long matchId) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}

		return matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				matchId
			));
	}

	/**
	 * 라인업 > 선수추가
	 * - 모달 최초 진입 시 해당 팀 선수 목록 조회
	 * - 현재 라인업 반영 여부(selected) 포함
	 */
	@Transactional(readOnly = true)
	public List<MatchLineupPlayerResponse> getSelectablePlayers(Long matchId, Long teamId) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (teamId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "teamId가 비어있습니다.");
		}

		MatchLineupDocument doc = getByMatchId(matchId);

		LineupSide side = getSideByTeamId(doc, teamId);

		if (side == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"팀 라인업을 찾을 수 없습니다.",
				"matchId=" + matchId + ", teamId=" + teamId
			);
		}

		Set<Long> selectedPlayerIds = extractSelectedPlayerIds(side);

		List<MatchLineupPlayerResponse> players =
			matchLineupSearchRepository.findPlayersByTeamId(teamId, 30);

		return players.stream()
			.map(player -> new MatchLineupPlayerResponse(
				player.getPlayerId(),
				player.getKName(),
				player.getEName(),
				player.getPosition(),
				player.getTeamId(),
				player.getTeamName(),
				player.getShirtNumber(),
				selectedPlayerIds.contains(player.getPlayerId())
			))
			.toList();
	}

	/**
	 * 라인업 > 선수추가
	 * - 검색 시 전체 선수 풀에서 조회
	 * - 현재 라인업 반영 여부(selected) 포함
	 */
	@Transactional(readOnly = true)
	public List<MatchLineupPlayerResponse> searchSelectablePlayers(Long matchId, Long teamId, String keyword) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (teamId == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"teamId가 비어있습니다."
			);
		}
		if (keyword == null || keyword.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "keyword가 비어있습니다.");
		}

		String trimmedKeyword = keyword.trim();

		MatchLineupDocument doc = getByMatchId(matchId);

		LineupSide side = getSideByTeamId(doc, teamId);

		if (side == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"팀 라인업을 찾을 수 없습니다.",
				"matchId=" + matchId + ", teamId=" + teamId
			);
		}

		Set<Long> selectedPlayerIds =
			extractSelectedPlayerIds(side);

		List<MatchLineupPlayerResponse> players =
			matchLineupSearchRepository.searchSelectablePlayers(
				trimmedKeyword,
				30
			);

		return players.stream()
			.map(player -> new MatchLineupPlayerResponse(
				player.getPlayerId(),
				player.getKName(),
				player.getEName(),
				player.getPosition(),
				player.getTeamId(),
				player.getTeamName(),
				player.getShirtNumber(),
				selectedPlayerIds.contains(player.getPlayerId())
			))
			.toList();
	}

	private LineupSide getSideByTeamId(MatchLineupDocument doc, Long teamId) {
		if (doc.getHome() != null && teamId.equals(doc.getHome().getTeamId())) {
			return doc.getHome();
		}
		if (doc.getAway() != null && teamId.equals(doc.getAway().getTeamId())) {
			return doc.getAway();
		}
		return null;
	}

	private Set<Long> extractSelectedPlayerIds(LineupSide side) {
		Set<Long> selectedPlayerIds = new HashSet<>();

		if (side == null) {
			return selectedPlayerIds;
		}

		if (side.getStartingLineup() != null) {
			side.getStartingLineup().stream()
				.map(LineupPlayer::getPlayerId)
				.filter(Objects::nonNull)
				.forEach(selectedPlayerIds::add);
		}

		if (side.getSubstitutes() != null) {
			side.getSubstitutes().stream()
				.map(LineupPlayer::getPlayerId)
				.filter(Objects::nonNull)
				.forEach(selectedPlayerIds::add);
		}

		return selectedPlayerIds;
	}

}
