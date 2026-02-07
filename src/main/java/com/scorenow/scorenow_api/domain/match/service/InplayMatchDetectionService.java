package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.dto.InplayMatchDetectionDto;
import com.scorenow.scorenow_api.domain.match.entity.InplayMatchDetectionMarked;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.InplayMatchDetectionMarkedRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InplayMatchDetectionService {

	private final MatchRepository matchRepo;
	private final InplayMatchDetectionMarkedRepository markedRepo;

	/** 전체 IN_PLAY MATCHES 조회 */
	@Transactional(readOnly = true)
	public List<InplayMatchDetectionDto> getInplayMatches() {

		return matchRepo.findInplayMatchDtos(MatchStatus.IN_PLAY);
	}

	/** 신규 IN_PLAY MATCHES 조회 */
	@Transactional
	public List<InplayMatchDetectionDto> getNewInplayMatchesAndMark() {
		List<InplayMatchDetectionDto> inplay = matchRepo.findInplayMatchDtos(MatchStatus.IN_PLAY);
		if (inplay.isEmpty())
			return List.of();

		List<String> ids = inplay.stream()
			.map(InplayMatchDetectionDto::getMatchId)
			.toList();

		Set<String> markedIds = markedRepo.findAllById(ids).stream()
			.map(InplayMatchDetectionMarked::getMatchId)
			.collect(Collectors.toSet());

		List<InplayMatchDetectionDto> newOnes = inplay.stream()
			.filter(dto -> !markedIds.contains(dto.getMatchId()))
			.toList();

		if (!newOnes.isEmpty()) {
			List<InplayMatchDetectionMarked> toSave = newOnes.stream()
				.map(dto -> new InplayMatchDetectionMarked(dto.getMatchId()))
				.toList();
			markedRepo.saveAll(toSave);
		}

		return newOnes;
	}
}
