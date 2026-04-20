package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupUpdateRequest;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchLineupCommandService {

	private final MatchLineupRepository matchLineupRepo;

	/** 라인업에 선수 반영 */
	@Transactional
	public String addPlayerToLineup(String matchId, String teamId, LineupPlayer player) {
		if (matchId == null || matchId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (teamId == null || teamId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "teamId가 비어있습니다.");
		}
		if (player == null || player.getPlayerId() == null || player.getPlayerId().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "player가 올바르지 않습니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND, "라인업이 존재하지 않습니다.", matchId
			));

		LineupSide side = findSide(doc, teamId);

		if (side == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"팀 라인업을 찾을 수 없습니다."
			);
		}

		boolean exists = isPlayerAlreadyInLineup(side, player.getPlayerId());
		if (exists) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"이미 라인업에 존재하는 선수입니다."
			);
		}

		if (player.isSubstitute()) {
			side.getSubstitutes().add(player);
		} else {
			side.getStartingLineup().add(player);
		}

		matchLineupRepo.save(doc);

		return matchId + ":" + player.getPlayerId();

	}

	/** 라인업에 선수 해제 */
	@Transactional
	public String removePlayerFromLineup(String matchId, String playerId) {
		if (matchId == null || matchId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (playerId == null || playerId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "playerId가 비어있습니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				matchId
			));

		boolean removed = false;

		if (doc.getHome() != null) {
			removed = removePlayerOnSide(doc.getHome(), playerId);
		}
		if (!removed && doc.getAway() != null) {
			removed = removePlayerOnSide(doc.getAway(), playerId);
		}

		if (!removed) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"라인업에서 선수를 찾지 못했습니다.",
				"matchId=" + matchId + ", playerId=" + playerId
			);
		}

		matchLineupRepo.save(doc);
		return matchId + ":" + playerId;
	}

	private LineupSide findSide(MatchLineupDocument doc, String teamId) {
		if (doc.getHome() != null && teamId.equals(doc.getHome().getTeamId())) {
			return doc.getHome();
		}
		if (doc.getAway() != null && teamId.equals(doc.getAway().getTeamId())) {
			return doc.getAway();
		}
		return null;
	}

	private boolean isPlayerAlreadyInLineup(LineupSide side, String playerId) {

		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				if (playerId.equals(p.getPlayerId())) {
					return true;
				}
			}
		}

		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				if (playerId.equals(p.getPlayerId())) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean removePlayerOnSide(LineupSide side, String playerId) {
		if (side == null) {
			return false;
		}

		if (side.getStartingLineup() != null) {
			boolean removed = side.getStartingLineup()
				.removeIf(p -> playerId.equals(p.getPlayerId()));
			if (removed) {
				return true;
			}
		}

		if (side.getSubstitutes() != null) {
			return side.getSubstitutes()
				.removeIf(p -> playerId.equals(p.getPlayerId()));
		}

		return false;
	}

	/**
	 * 관리자가 특정 경기의 라인업 선수 정보를 수정
	 * - matchId로 라인업 doc 조회
	 * - home/away에서 playerId 조회
	 * - request에 들어온 필드만 업데이트
	 * - 변경 없으면 NO_CHANGES, 변경 있으면 저장 후 "matchId:playerId" 반환
	 */
	@Transactional
	public String updateLineupManual(String matchId, String playerId, MatchLineupUpdateRequest request) {

		if (matchId == null || matchId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (playerId == null || playerId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "playerId가 비어있습니다.");
		}
		if (request == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "request가 null입니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				matchId
			));

		UpdateResult result = UpdateResult.notFound();

		if (doc.getHome() != null) {
			result = updatePlayerOnSide(doc.getHome(), playerId, request);
		}
		if (!result.found && doc.getAway() != null) {
			result = updatePlayerOnSide(doc.getAway(), playerId, request);
		}

		if (!result.found) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"라인업에서 선수를 찾지 못했습니다.",
				"matchId=" + matchId + ", playerId=" + playerId
			);
		}

		if (!result.changed) {
			return "NO_CHANGES";
		}

		matchLineupRepo.save(doc);
		return matchId + ":" + playerId;
	}

	/** 업데이트 결과(찾음/변경 여부) */
	private static class UpdateResult {
		final boolean found;
		final boolean changed;

		UpdateResult(boolean found, boolean changed) {
			this.found = found;
			this.changed = changed;
		}

		static UpdateResult notFound() {
			return new UpdateResult(false, false);
		}

		static UpdateResult foundNoChange() {
			return new UpdateResult(true, false);
		}

		static UpdateResult foundChanged() {
			return new UpdateResult(true, true);
		}
	}

	/** home/away에서 playerId 선수를 찾아 request 필드만 부분 업데이트 */
	private UpdateResult updatePlayerOnSide(LineupSide side, String playerId, MatchLineupUpdateRequest req) {
		if (side == null)
			return UpdateResult.notFound();

		LineupPlayer p = findPlayer(side, playerId);
		if (p == null)
			return UpdateResult.notFound();

		boolean changed = false;

		if (req.getPosition() != null && !req.getPosition().isBlank()) {
			String newPos = req.getPosition().trim();
			if (p.getPosition() == null || !p.getPosition().equals(newPos)) {
				p.setPosition(newPos);
				changed = true;
			}
		}

		if (req.getShirtNumber() != null && !req.getShirtNumber().isBlank()) {
			String newNo = req.getShirtNumber().trim();
			if (p.getShirtNumber() == null || !p.getShirtNumber().equals(newNo)) {
				p.setShirtNumber(newNo);
				changed = true;
			}
		}

		if (req.getGoals() != null) {
			int newGoals = Math.max(0, req.getGoals());
			Integer cur = (p.getGoals() == null ? 0 : p.getGoals());
			if (!cur.equals(newGoals)) {
				p.setGoals(newGoals);
				changed = true;
			}
		}

		return changed ? UpdateResult.foundChanged() : UpdateResult.foundNoChange();
	}

	/** startingLineup/substitutes에서 playerId 일치 선수 검색 */
	private LineupPlayer findPlayer(LineupSide side, String playerId) {
		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				if (playerId.equals(p.getPlayerId()))
					return p;
			}
		}
		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				if (playerId.equals(p.getPlayerId()))
					return p;
			}
		}
		return null;
	}
}
