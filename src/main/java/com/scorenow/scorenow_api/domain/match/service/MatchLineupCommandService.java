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
	public String addPlayerToLineup(Long matchId, Long teamId, LineupPlayer player) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (teamId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "teamId가 비어있습니다.");
		}
		if (player == null || player.getPlayerId() == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "player가 올바르지 않습니다.");
		}
		if (player.getEName() == null || player.getEName().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "eName이 필요합니다.");
		}
		if (player.getShirtNumber() == null || player.getShirtNumber().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "shirtNumber가 필요합니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				"matchId=" + matchId
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
				ErrorCode.MATCH_LINEUP_PLAYER_ALREADY_EXISTS,
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
	public String removePlayerFromLineup(Long matchId, Long playerId) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (playerId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "playerId가 비어있습니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				"matchId=" + matchId
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

	private LineupSide findSide(MatchLineupDocument doc, Long teamId) {
		if (doc.getHome() != null && teamId.equals(doc.getHome().getTeamId())) {
			return doc.getHome();
		}
		if (doc.getAway() != null && teamId.equals(doc.getAway().getTeamId())) {
			return doc.getAway();
		}
		return null;
	}

	private boolean isPlayerAlreadyInLineup(LineupSide side, Long playerId) {
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

	private boolean removePlayerOnSide(LineupSide side, Long playerId) {
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

	@Transactional
	public String updateLineupManual(Long matchId, String apiPlayerId, MatchLineupUpdateRequest request) {
		if (matchId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}
		if (apiPlayerId == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "playerId가 비어있습니다.");
		}
		if (request == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "request가 null입니다.");
		}

		MatchLineupDocument doc = matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				"matchId=" + matchId
			));

		UpdateResult result = UpdateResult.notFound();

		if (doc.getHome() != null) {
			result = updatePlayerOnSide(doc.getHome(), apiPlayerId, request);
		}
		if (!result.found && doc.getAway() != null) {
			result = updatePlayerOnSide(doc.getAway(), apiPlayerId, request);
		}

		if (!result.found) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"라인업에서 선수를 찾지 못했습니다.",
				"matchId=" + matchId + ", apiPlayerId=" + apiPlayerId
			);
		}

		if (!result.changed) {
			return "NO_CHANGES";
		}

		matchLineupRepo.save(doc);
		return matchId + ":" + apiPlayerId;
	}

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

	private UpdateResult updatePlayerOnSide(LineupSide side, String apiPlayerId, MatchLineupUpdateRequest req) {
		if (side == null) {
			return UpdateResult.notFound();
		}

		LineupPlayer p = findPlayer(side, apiPlayerId);
		if (p == null) {
			return UpdateResult.notFound();
		}

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

	private LineupPlayer findPlayer(LineupSide side, String apiPlayerId) {
		if (side.getStartingLineup() != null) {
			for (LineupPlayer p : side.getStartingLineup()) {
				if (apiPlayerId.equals(p.getApiPlayerId())) {
					return p;
				}
			}
		}
		if (side.getSubstitutes() != null) {
			for (LineupPlayer p : side.getSubstitutes()) {
				if (apiPlayerId.equals(p.getApiPlayerId())) {
					return p;
				}
			}
		}
		return null;
	}
}