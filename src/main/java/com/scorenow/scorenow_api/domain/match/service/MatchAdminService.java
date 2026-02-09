package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
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

	/**
	 * 경기 리스트 조회
	 */
	public Page<MatchListResponse> getMatches(MatchSearchCondition condition, Pageable pageable) {
		Page<Match> matches = matchRepository.searchMatches(condition, pageable);

		// Fetch 조인 적용
		List<MatchListResponse> content = matches.getContent().stream()
			.map(this::toListResponseWithFetch)
			.toList();

		return new PageImpl<>(content, pageable, matches.getTotalElements());
	}


	/**
	 * 경기 수동 등록
	 */
	@Transactional
	public MatchListResponse createMatch(MatchCreateRequest request) {
		// 연관 엔티티 존재 확인
		fetchLeague(request.getLeagueId(), "리그 정보를 찾을 수 없습니다.");
		fetchTeam(request.getHomeId(), "홈팀 정보를 찾을 수 없습니다.");
		fetchTeam(request.getAwayId(), "원정팀 정보를 찾을 수 없습니다.");
		if (request.getSportId() != null) {
			fetchSport(request.getSportId(), "스포츠 정보를 찾을 수 없습니다.");
		}

		// 수동 경기 생성
		Match match = Match.builder()
			.id(Match.generateManualId(request.getSportId()))
			.sportId(request.getSportId())
			.leagueId(request.getLeagueId())
			.homeId(request.getHomeId())
			.awayId(request.getAwayId())
			.startAt(request.getStartAt())
			.statusCode(MatchStatus.NOT_STARTED)
			.matchType("M")
			.isManual(true)
			.isActive(false)
			.build();

		matchRepository.save(match);
		log.info("수동 경기 등록 완료 - matchId: {}", match.getId());

		// 기본 정보만 반환
		return toBasicResponse(match);
	}

	/**
	 * 경기 수정
	 */
	@Transactional
	public void updateMatch(String matchId, MatchUpdateRequest request) {
		Match match = findMatchById(matchId);

		// 시작 시간 수정
		if (request.getStartAt() != null) {
			match.updateStartAt(request.getStartAt());
		}

		// 경기 상태 수정
		if (request.getStatusCode() != null) {
			try {
				MatchStatus status = MatchStatus.valueOf(request.getStatusCode());
				match.updateStatus(status);
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
			}
		}

		// 점수 수정
		if (request.getHomeScore() != null) {
			match.updateHomeScore(request.getHomeScore());
		}
		if (request.getAwayScore() != null) {
			match.updateAwayScore(request.getAwayScore());
		}

		// 사용 여부 수정
		if (request.getIsActive() != null) {
			match.updateIsActive(request.getIsActive());
		}

		matchRepository.save(match);
		log.info("경기 수정 완료 - matchId: {}", matchId);
	}

	/**
	 * 경기 삭제
	 */
	@Transactional
	public void deleteMatch(String matchId) {
		Match match = findMatchById(matchId);

		// 수동 경기만 삭제 가능
		if (!match.isManual()) {
			throw new BusinessException(ErrorCode.MATCH_CANNOT_DELETE, "자동 경기는 삭제할 수 없습니다.");
		}

		matchRepository.delete(match);
		log.info("경기 삭제 완료 - matchId: {}", matchId);
	}

	// ======================== 헬퍼 메서드 ========================

	/**
	 * 경기 조회 (공통)
	 */
	private Match findMatchById(String matchId) {
		return matchRepository.findById(matchId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
	}

	/**
	 * 리그 조회 (존재하지 않으면 예외 발생)
	 */
	private void fetchLeague(String leagueId, String errorMessage) {
		leagueRepository.findById(leagueId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND, errorMessage));
	}

	/**
	 * 팀 조회 (존재하지 않으면 예외 발생)
	 */
	private void fetchTeam(String teamId, String errorMessage) {
		teamRepository.findById(teamId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, errorMessage));
	}

	/**
	 * 스포츠 조회 (존재하지 않으면 예외 발생)
	 */
	private void fetchSport(String sportId, String errorMessage) {
		sportRepository.findById(sportId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND, errorMessage));
	}

	// ======================== 응답 변환 메서드 ========================

	/**
	 * Match -> MatchListResponse 변환
	 */
	private MatchListResponse toListResponseWithFetch(Match match) {

		return MatchListResponse.builder()
			.id(match.getId())
			.sportId(match.getSportId())
			.sportName(match.getSport() != null ? match.getSport().getEName() : "")
			.leagueId(match.getLeagueId())
			.leagueName(match.getLeague() != null ? match.getLeague().getEName() : "")
			.matchType(match.getMatchType())
			.startAt(match.getStartAt())
			.statusCode(match.getStatusCode().name())
			.statusName(match.getStatusCode().getDescription())
			.homeId(match.getHomeId())
			.homeName(match.getHomeTeam() != null ? match.getHomeTeam().getEName() : "")
			.homeImageUrl(match.getHomeTeam() != null ? match.getHomeTeam().getImageUrl() : null)
			.homeScore(match.getHomeScore())
			.awayId(match.getAwayId())
			.awayName(match.getAwayTeam() != null ? match.getAwayTeam().getEName() : "")
			.awayImageUrl(match.getAwayTeam() != null ? match.getAwayTeam().getImageUrl() : null)
			.awayScore(match.getAwayScore())
			.isManual(match.isManual())
			.isActive(match.isActive())
			.build();
	}

	/**
	 * Match -> MatchListResponse 변환 (기본 정보만 반환)
	 */
	private MatchListResponse toBasicResponse(Match match) {
		return buildMatchResponse(match, null, null, null, null);
	}

	/**
	 * MatchListResponse 생성
	 */
	private MatchListResponse buildMatchResponse(Match match,
		League leagueEntity, Team homeTeam, Team awayTeam, Sport sportEntity) {

		String leagueName = leagueEntity != null ? leagueEntity.getEName() : "";
		String sportName = sportEntity != null ? sportEntity.getEName() : "";

		return MatchListResponse.builder()
			.id(match.getId())
			.sportId(match.getSportId())
			.sportName(sportName)
			.leagueId(match.getLeagueId())
			.leagueName(leagueName)
			.matchType(match.getMatchType())
			.startAt(match.getStartAt())
			.statusCode(match.getStatusCode().name())
			.statusName(match.getStatusCode().getDescription())
			.homeId(match.getHomeId())
			.homeName(homeTeam != null ? homeTeam.getEName() : "")
			.homeImageUrl(homeTeam != null ? homeTeam.getImageUrl() : null)
			.homeScore(match.getHomeScore())
			.awayId(match.getAwayId())
			.awayName(awayTeam != null ? awayTeam.getEName() : "")
			.awayImageUrl(awayTeam != null ? awayTeam.getImageUrl() : null)
			.awayScore(match.getAwayScore())
			.isManual(match.isManual())
			.isActive(match.isActive())
			.build();
	}


}
