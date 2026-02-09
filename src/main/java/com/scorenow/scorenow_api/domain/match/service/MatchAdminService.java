package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.entity.MatchType;
import com.scorenow.scorenow_api.domain.match.mapper.MatchMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchAdminService {

	private final MatchRepository matchRepository;
	private final LeagueRepository leagueRepository;
	private final TeamRepository teamRepository;
	private final SportRepository sportRepository;
	private final MatchMapper matchMapper;

	/**
	 * 경기 리스트 조회
	 */
	public Page<MatchListResponse> getMatches(MatchSearchCondition condition, Pageable pageable) {
		return matchRepository.searchMatches(condition, pageable)
			.map(matchMapper::toResponse);
	}

	/**
	 * 경기 수동 등록
	 */
	@Transactional
	public MatchListResponse createMatch(MatchCreateRequest request) {
		validateMatchCreateRequest(request);

		Match match = matchMapper.toEntity(request);
		matchRepository.save(match);

		log.info("수동 경기 등록 완료 - matchId: {}", match.getId());

		return matchRepository.findByIdWithRelations(match.getId())
			.map(matchMapper::toResponse)
			.orElseGet(() -> matchMapper.toResponse(match));
	}

	/**
	 * 경기 수정
	 */
	@Transactional
	public void updateMatch(String matchId, MatchUpdateRequest request) {
		Match match = findMatchById(matchId);

		applyUpdates(match, request);

		log.info("경기 수정 완료 - matchId: {}", matchId);
	}

	/**
	 * 경기 삭제
	 */
	@Transactional
	public void deleteMatch(String matchId) {
		Match match = findMatchById(matchId);

		validateDeletable(match);

		matchRepository.delete(match);
		log.info("경기 삭제 완료 - matchId: {}", matchId);
	}

	// === Validation Methods ===

	private void validateMatchCreateRequest(MatchCreateRequest request) {
		try{
			MatchType.fromCode(request.getMatchType());
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.MATCH_INVALID_TYPE);
		}
		if (!leagueRepository.existsById(request.getLeagueId())) {
			throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
		}
		if (!teamRepository.existsById(request.getHomeId())) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "홈팀을 찾을 수 없습니다.");
		}
		if (!teamRepository.existsById(request.getAwayId())) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "원정팀을 찾을 수 없습니다.");
		}
		if (request.getSportId() != null && !sportRepository.existsById(request.getSportId())) {
			throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
		}
	}

	private void validateDeletable(Match match) {
		if (!match.isDeletable()) {
			throw new BusinessException(ErrorCode.MATCH_CANNOT_DELETE, "자동 경기는 삭제할 수 없습니다.");
		}
	}

	// === Helper Methods ===

	private Match findMatchById(String matchId) {
		return matchRepository.findById(matchId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
	}

	private void applyUpdates(Match match, MatchUpdateRequest request) {
		if (request.getStartAt() != null) {
			match.updateStartAt(request.getStartAt());
		}
		if (request.getStatusCode() != null) {
			try {
				match.updateStatus(MatchStatus.valueOf(request.getStatusCode()));
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
			}
		}
		if (request.getHomeScore() != null) {
			match.updateHomeScore(request.getHomeScore());
		}
		if (request.getAwayScore() != null) {
			match.updateAwayScore(request.getAwayScore());
		}
		if (request.getIsActive() != null) {
			match.updateIsActive(request.getIsActive());
		}
	}



}
