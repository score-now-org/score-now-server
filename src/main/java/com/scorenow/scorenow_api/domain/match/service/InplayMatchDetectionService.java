package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.InplayMatchDetectionDto;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InplayMatchDetectionService {

	private final MatchRepository matchRepo;
	private final MatchLineupRepository lineupDocRepo;

	/** 전체 IN_PLAY MATCHES 조회 */
	@Transactional(readOnly = true)
	public List<InplayMatchDetectionDto> getInplayMatches() {

		return matchRepo.findInplayMatchDtos(MatchStatus.IN_PLAY);
	}

	/** 신규 IN_PLAY MATCHES 조회 */
	@Transactional
	public List<InplayMatchDetectionDto> getNewInplayMatches() {
		List<InplayMatchDetectionDto> inplay = matchRepo.findInplayMatchDtos(MatchStatus.IN_PLAY);
		if (inplay.isEmpty())
			return List.of();

		List<String> ids = inplay.stream()
			.map(InplayMatchDetectionDto::getMatchId)
			.toList();

		Set<String> existingDocIds = lineupDocRepo.findAllById(ids).stream()
			.map(MatchLineupDocument::getId)
			.collect(Collectors.toSet());

		return inplay.stream()
			.filter(dto -> !existingDocIds.contains(dto.getMatchId()))
			.toList();

	}
}
