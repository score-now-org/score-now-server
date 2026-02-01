package com.scorenow.scorenow_api.domain.match.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.MatchLineupRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchLineupService {

	private final BetsApiClient betsApiClient;
	private final MatchLineupRepository matchLineupRepository;

	/**
	 * eventId 기준으로 라인업 조회 + Mongo 저장 (upsert)
	 * matchId 예: BETS1 + eventId
	 */
	@Transactional
	public MatchLineupDocument fetchAndSaveByEventId(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("eventId is blank");
		}

		String matchId = "BETS1" + eventId;

		BetsLineupResponse response = betsApiClient.getLineup(eventId);
		validateResponse(response, eventId);

		BetsLineupResponse.BetsLineupResult results = response.getResults();

		MatchLineupDocument doc = matchLineupRepository.findById(matchId)
			.orElseGet(() -> MatchLineupDocument.create(matchId));

		doc.setHome(mapSide(results.getHome()));
		doc.setAway(mapSide(results.getAway()));

		return matchLineupRepository.save(doc);
	}

	@Transactional(readOnly = true)
	public MatchLineupDocument getByMatchId(String matchId) {
		return matchLineupRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("라인업이 존재하지 않습니다. matchId=" + matchId));
	}

	/* ========== */
	private void validateResponse(BetsLineupResponse response, String eventId) {
		if (response == null) {
			throw new IllegalStateException("라인업 API 응답이 null 입니다. eventId=" + eventId);
		}
		if (response.getSuccess() == null || response.getSuccess() != 1) {
			throw new IllegalStateException("라인업 API 실패. success=" + response.getSuccess() + ", eventId=" + eventId);
		}
		if (response.getResults() == null
			|| response.getResults().getHome() == null
			|| response.getResults().getAway() == null) {
			throw new IllegalStateException("라인업 API results/home/away가 비어있습니다. eventId=" + eventId);
		}
	}

	private LineupSide mapSide(BetsLineupResponse.BetsLineupSide ext) {
		return LineupSide.builder()
			.formation(ext.getFormation())
			.startingLineup(mapPlayers(ext.getStartinglineup()))
			.substitutes(mapPlayers(ext.getSubstitutes()))
			.build();
	}

	private List<LineupPlayer> mapPlayers(List<BetsLineupResponse.BetsLineupPlayer> extPlayers) {
		if (extPlayers == null) {
			return new ArrayList<>();
		}
		List<LineupPlayer> list = new ArrayList<>();
		for (BetsLineupResponse.BetsLineupPlayer ext : extPlayers) {
			list.add(mapPlayer(ext));
		}
		return list;
	}

	/* TODO: view api 골득점 업데이트 */
	private LineupPlayer mapPlayer(BetsLineupResponse.BetsLineupPlayer ext) {
		String playerId = null;
		String eName = null;

		if (ext != null && ext.getPlayer() != null) {
			playerId = ext.getPlayer().getId();
			eName = ext.getPlayer().getName();
		}

		return LineupPlayer.builder()
			.playerId(playerId)
			.eName(eName)
			.shirtNumber(parseIntOrNull(ext != null ? ext.getShirtnumber() : null))
			.position(mapPosition(ext != null ? ext.getPos() : null))
			.goals(0) // view api 연동 전
			.build();
	}

	private Integer parseIntOrNull(String v) {
		if (v == null || v.isBlank())
			return null;
		try {
			return Integer.valueOf(v);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String mapPosition(String pos) {
		if (pos == null)
			return null;

		return switch (pos) {
			case "Guard" -> "GK";
			case "Defender" -> "DF";
			case "Midfielder" -> "MF";
			case "Forward" -> "FW";
			default -> "SUB";
		};
	}

	/* TODO: matchId를 입력으로 받을 때 사용 */
	private String extractEventId(String matchId) {
		if (!matchId.startsWith("BETS")) {
			throw new IllegalArgumentException("Invalid matchId format: " + matchId);
		}
		return matchId.replaceFirst("^BETS\\d+", "");
	}
}
