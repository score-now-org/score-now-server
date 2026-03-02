package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchLineupQueryService {

	private final MatchLineupRepository matchLineupRepo;

	/** matchId 기준으로 라인업 조회 */
	@Transactional(readOnly = true)
	public MatchLineupDocument getByMatchId(String matchId) {

		if (matchId == null || matchId.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER, "matchId가 비어있습니다.");
		}

		return matchLineupRepo.findById(matchId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.MATCH_LINEUP_NOT_FOUND,
				"라인업이 존재하지 않습니다.",
				matchId
			));
	}
}
