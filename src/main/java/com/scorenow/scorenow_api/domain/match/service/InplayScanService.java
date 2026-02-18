package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.match.dto.InplayScanDto;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InplayScanService {

	private final MatchRepository matchRepo;

	@Transactional(readOnly = true)
	public List<InplayScanDto> scanInplayMatches() {
		return matchRepo.findInplayMatchDtos(MatchStatus.IN_PLAY);
	}
}