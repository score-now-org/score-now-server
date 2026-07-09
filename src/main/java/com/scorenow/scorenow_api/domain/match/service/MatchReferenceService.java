package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchReferenceService {

	private final MatchRepository matchRepository;

	public Match requireMatch(Long matchId) {
		return matchRepository.findById(matchId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
	}
}