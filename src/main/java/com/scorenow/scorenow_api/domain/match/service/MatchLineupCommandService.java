package com.scorenow.scorenow_api.domain.match.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerAddRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
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
public class MatchLineupCommandService {

	private final MatchLineupRepository matchLineupRepo;
	private final MatchLineupSearchRepository matchLineupSearchRepository;

	/**
	 * 라인업에 선수 반영
	 */
	@Transactional
	public String addPlayerToLineup(
		Long matchId,
		Long teamId,
		MatchLineupPlayerAddRequest request
	) {
		validateRequiredId(matchId, "matchId");
		validateRequiredId(teamId, "teamId");

		if (request == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"request가 비어있습니다."
			);
		}

		if (request.playerId() == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"playerId가 비어있습니다."
			);
		}

		if (request.role() == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"role이 비어있습니다."
			);
		}

		MatchLineupDocument document = getLineupDocument(matchId);
		LineupSide side = getLineupSide(document, teamId);

		Long playerId = request.playerId();

		if (isPlayerAlreadyInLineup(side, playerId)) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_ALREADY_EXISTS,
				"이미 라인업에 존재하는 선수입니다.",
				"matchId=" + matchId
					+ ", teamId=" + teamId
					+ ", playerId=" + playerId
			);
		}

		MatchLineupPlayerResponse playerInfo =
			matchLineupSearchRepository
				.findPlayerByIdAndTeamId(playerId, teamId)
				.orElseThrow(() -> new BusinessException(
					ErrorCode.INVALID_PARAMETER,
					"해당 팀의 선수 정보를 찾을 수 없습니다.",
					"teamId=" + teamId + ", playerId=" + playerId
				));

		LineupPlayer player = LineupPlayer.builder()
			.playerId(playerInfo.getPlayerId())
			.eName(playerInfo.getEName())
			.kName(playerInfo.getKName())
			.shirtNumber(playerInfo.getShirtNumber())
			.position(playerInfo.getPosition())
			.goals(0)
			.temp(false)
			.build();

		switch (request.role()) {
			case STARTER -> side.getStartingLineup().add(player);
			case SUBSTITUTE -> side.getSubstitutes().add(player);
		}

		matchLineupRepo.save(document);

		return matchId + ":" + playerId;
	}

	/**
	 * 라인업에서 선수 해제
	 */
	@Transactional
	public String removePlayerFromLineup(
		Long matchId,
		Long teamId,
		Long playerId
	) {
		validateRequiredId(matchId, "matchId");
		validateRequiredId(teamId, "teamId");
		validateRequiredId(playerId, "playerId");

		MatchLineupDocument document = getLineupDocument(matchId);
		LineupSide side = getLineupSide(document, teamId);

		boolean removed = removePlayerOnSide(side, playerId);

		if (!removed) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"해당 팀의 라인업에서 선수를 찾지 못했습니다.",
				"matchId=" + matchId
					+ ", teamId=" + teamId
					+ ", playerId=" + playerId
			);
		}

		matchLineupRepo.save(document);

		return matchId + ":" + playerId;
	}

	/**
	 * 라인업 팀 설정 수정
	 * - 포메이션, 유니폼 컬러 변경
	 */
	@Transactional
	public String updateLineupSide(
		Long matchId,
		Long teamId,
		MatchLineupSideUpdateRequest request
	) {
		validateRequiredId(matchId, "matchId");
		validateRequiredId(teamId, "teamId");

		if (request == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"request가 비어있습니다."
			);
		}

		if (request.getFormation() == null
			&& request.getUniformColor() == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"수정할 포메이션 또는 유니폼 컬러가 없습니다."
			);
		}

		MatchLineupDocument document = getLineupDocument(matchId);
		LineupSide side = getLineupSide(document, teamId);

		boolean changed = false;

		if (request.getFormation() != null) {
			String formation = request.getFormation().trim();

			if (formation.isBlank()) {
				throw new BusinessException(
					ErrorCode.INVALID_PARAMETER,
					"포메이션이 비어있습니다."
				);
			}

			if (!Objects.equals(side.getFormation(), formation)) {
				side.setFormation(formation);
				changed = true;
			}
		}

		if (request.getUniformColor() != null) {
			String uniformColor = request.getUniformColor().trim();

			if (uniformColor.isBlank()) {
				throw new BusinessException(
					ErrorCode.INVALID_PARAMETER,
					"유니폼 컬러가 비어있습니다."
				);
			}

			if (!Objects.equals(
				side.getUniformColor(),
				uniformColor
			)) {
				side.setUniformColor(uniformColor);
				changed = true;
			}
		}

		if (!changed) {
			return "NO_CHANGES";
		}

		matchLineupRepo.save(document);

		return matchId + ":" + teamId;
	}

	/**
	 * 라인업 선수 수정
	 * - 포지션, 등번호, 득점 수 변경
	 */
	@Transactional
	public String updateLineupManual(
		Long matchId,
		Long teamId,
		Long playerId,
		MatchLineupPlayerUpdateRequest request
	) {
		validateRequiredId(matchId, "matchId");
		validateRequiredId(teamId, "teamId");
		validateRequiredId(playerId, "playerId");

		if (request == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"request가 비어있습니다."
			);
		}

		MatchLineupDocument document = getLineupDocument(matchId);
		LineupSide side = getLineupSide(document, teamId);

		UpdateResult result = updatePlayerOnSide(
			side,
			playerId,
			request
		);

		if (!result.found) {
			throw new BusinessException(
				ErrorCode.MATCH_LINEUP_PLAYER_NOT_FOUND,
				"해당 팀의 라인업에서 선수를 찾지 못했습니다.",
				"matchId=" + matchId
					+ ", teamId=" + teamId
					+ ", playerId=" + playerId
			);
		}

		if (!result.changed) {
			return "NO_CHANGES";
		}

		matchLineupRepo.save(document);

		return matchId + ":" + playerId;
	}

	private MatchLineupDocument getLineupDocument(Long matchId) {
		return matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				"matchId=" + matchId
			));
	}

	private LineupSide getLineupSide(
		MatchLineupDocument document,
		Long teamId
	) {
		LineupSide side = findSide(document, teamId);

		if (side == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				"팀 라인업을 찾을 수 없습니다.",
				"matchId=" + document.getId()
					+ ", teamId=" + teamId
			);
		}

		return side;
	}

	private LineupSide findSide(
		MatchLineupDocument document,
		Long teamId
	) {
		if (document.getHome() != null
			&& Objects.equals(
			teamId,
			document.getHome().getTeamId()
		)) {
			return document.getHome();
		}

		if (document.getAway() != null
			&& Objects.equals(
			teamId,
			document.getAway().getTeamId()
		)) {
			return document.getAway();
		}

		return null;
	}

	private boolean isPlayerAlreadyInLineup(
		LineupSide side,
		Long playerId
	) {
		return containsPlayer(
			side.getStartingLineup(),
			playerId
		) || containsPlayer(
			side.getSubstitutes(),
			playerId
		);
	}

	private boolean containsPlayer(
		Iterable<LineupPlayer> players,
		Long playerId
	) {
		if (players == null) {
			return false;
		}

		for (LineupPlayer player : players) {
			if (Objects.equals(
				playerId,
				player.getPlayerId()
			)) {
				return true;
			}
		}

		return false;
	}

	private boolean removePlayerOnSide(
		LineupSide side,
		Long playerId
	) {
		if (side.getStartingLineup() != null) {
			boolean removed = side.getStartingLineup()
				.removeIf(player ->
					Objects.equals(
						playerId,
						player.getPlayerId()
					)
				);

			if (removed) {
				return true;
			}
		}

		if (side.getSubstitutes() != null) {
			return side.getSubstitutes()
				.removeIf(player ->
					Objects.equals(
						playerId,
						player.getPlayerId()
					)
				);
		}

		return false;
	}

	private UpdateResult updatePlayerOnSide(
		LineupSide side,
		Long playerId,
		MatchLineupPlayerUpdateRequest request
	) {
		LineupPlayer player = findPlayer(side, playerId);

		if (player == null) {
			return UpdateResult.notFound();
		}

		boolean changed = false;

		if (request.getPosition() != null
			&& !request.getPosition().isBlank()) {
			String newPosition = request.getPosition().trim();

			if (!Objects.equals(
				player.getPosition(),
				newPosition
			)) {
				player.setPosition(newPosition);
				changed = true;
			}
		}

		if (request.getShirtNumber() != null
			&& !request.getShirtNumber().isBlank()) {
			String newShirtNumber =
				request.getShirtNumber().trim();

			if (!Objects.equals(
				player.getShirtNumber(),
				newShirtNumber
			)) {
				player.setShirtNumber(newShirtNumber);
				changed = true;
			}
		}

		if (request.getGoals() != null) {
			int newGoals = Math.max(0, request.getGoals());
			int currentGoals = player.getGoals() == null
				? 0
				: player.getGoals();

			if (currentGoals != newGoals) {
				player.setGoals(newGoals);
				changed = true;
			}
		}

		return changed
			? UpdateResult.foundChanged()
			: UpdateResult.foundNoChange();
	}

	private LineupPlayer findPlayer(
		LineupSide side,
		Long playerId
	) {
		LineupPlayer player = findPlayer(
			side.getStartingLineup(),
			playerId
		);

		if (player != null) {
			return player;
		}

		return findPlayer(
			side.getSubstitutes(),
			playerId
		);
	}

	private LineupPlayer findPlayer(
		Iterable<LineupPlayer> players,
		Long playerId
	) {
		if (players == null) {
			return null;
		}

		for (LineupPlayer player : players) {
			if (Objects.equals(
				playerId,
				player.getPlayerId()
			)) {
				return player;
			}
		}

		return null;
	}

	private void validateRequiredId(
		Long value,
		String fieldName
	) {
		if (value == null) {
			throw new BusinessException(
				ErrorCode.INVALID_PARAMETER,
				fieldName + "가 비어있습니다."
			);
		}
	}

	private static class UpdateResult {

		private final boolean found;
		private final boolean changed;

		private UpdateResult(
			boolean found,
			boolean changed
		) {
			this.found = found;
			this.changed = changed;
		}

		private static UpdateResult notFound() {
			return new UpdateResult(false, false);
		}

		private static UpdateResult foundNoChange() {
			return new UpdateResult(true, false);
		}

		private static UpdateResult foundChanged() {
			return new UpdateResult(true, true);
		}
	}
}