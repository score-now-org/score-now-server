package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		List<Match> matchList = matches.getContent();

		// ID 일괄 수집
		Set<String> leagueIds = matchList.stream().map(Match::getLeagueId).filter(id -> id != null).collect(Collectors.toSet());
		Set<String> teamIds = matchList.stream()
			.flatMap(m -> Stream.of(m.getHomeId(), m.getAwayId()))
			.filter(id -> id != null)
			.collect(Collectors.toSet());
		Set<String> sportIds = matchList.stream().map(Match::getSportId).filter(id -> id != null).collect(Collectors.toSet());

		// 일괄 조회
		Map<String, League> leagueMap = leagueRepository.findAllById(leagueIds).stream()
			.collect(Collectors.toMap(League::getId, Function.identity()));
		Map<String, Team> teamMap = teamRepository.findAllById(teamIds).stream()
			.collect(Collectors.toMap(Team::getId, Function.identity()));
		Map<String, Sport> sportMap = sportRepository.findAllById(sportIds).stream()
			.collect(Collectors.toMap(Sport::getId, Function.identity()));

		List<MatchListResponse> content = matchList.stream()
			.map(match -> toListResponse(match, false, leagueMap, teamMap, sportMap))
			.toList();

		return new PageImpl<>(content, pageable, matches.getTotalElements());
	}

	/**
	 * 경기 상세 조회
	 */
	public MatchListResponse getMatch(String matchId) {
		Match match = matchRepository.findById(matchId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

		League leagueEntity = match.getLeagueId() != null ? leagueRepository.findById(match.getLeagueId()).orElse(null) : null;
		Team homeTeam = match.getHomeId() != null ? teamRepository.findById(match.getHomeId()).orElse(null) : null;
		Team awayTeam = match.getAwayId() != null ? teamRepository.findById(match.getAwayId()).orElse(null) : null;
		Sport sportEntity = match.getSportId() != null ? sportRepository.findById(match.getSportId()).orElse(null) : null;

		return toListResponse(match, true, leagueEntity, homeTeam, awayTeam, sportEntity);
	}

	/**
	 * 경기 수동 등록
	 */
	@Transactional
	public MatchListResponse createMatch(MatchCreateRequest request) {
		// 리그 존재 확인
		if (!leagueRepository.existsById(request.getLeagueId())) {
			throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
		}

		// 홈팀 존재 확인
		if (!teamRepository.existsById(request.getHomeId())) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "홈팀을 찾을 수 없습니다.");
		}

		// 원정팀 존재 확인
		if (!teamRepository.existsById(request.getAwayId())) {
			throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "원정팀을 찾을 수 없습니다.");
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

		League leagueEntity = leagueRepository.findById(request.getLeagueId()).orElse(null);
		Team homeTeam = teamRepository.findById(request.getHomeId()).orElse(null);
		Team awayTeam = teamRepository.findById(request.getAwayId()).orElse(null);
		Sport sportEntity = request.getSportId() != null ? sportRepository.findById(request.getSportId()).orElse(null) : null;

		return toListResponse(match, true, leagueEntity, homeTeam, awayTeam, sportEntity);
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

	/**
	 * 경기 조회(공통)
	 */
	private Match findMatchById(String matchId) {
		return matchRepository.findById(matchId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
	}

	/**
	 * Match -> MatchListResponse 변환 (리스트 조회용, 일괄 조회된 Map 사용)
	 */
	private MatchListResponse toListResponse(Match match, boolean includeDetail,
		Map<String, League> leagueMap, Map<String, Team> teamMap, Map<String, Sport> sportMap) {

		League leagueEntity = match.getLeagueId() != null ? leagueMap.get(match.getLeagueId()) : null;
		Team homeTeam = match.getHomeId() != null ? teamMap.get(match.getHomeId()) : null;
		Team awayTeam = match.getAwayId() != null ? teamMap.get(match.getAwayId()) : null;
		Sport sportEntity = match.getSportId() != null ? sportMap.get(match.getSportId()) : null;

		return toListResponse(match, includeDetail, leagueEntity, homeTeam, awayTeam, sportEntity);
	}

	/**
	 * Match -> MatchListResponse 변환 (상세 조회용)
	 */
	private MatchListResponse toListResponse(Match match, boolean includeDetail,
		League leagueEntity, Team homeTeam, Team awayTeam, Sport sportEntity) {

		String leagueName = leagueEntity != null ? leagueEntity.getEName() : "";
		String sportName = sportEntity != null ? sportEntity.getKName() : "";

		MatchListResponse.MatchListResponseBuilder builder = MatchListResponse.builder()
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
			.isActive(match.isActive());

		if (includeDetail) {
			builder
				.betsApiEventId(match.getBetsApiEventId())
				.bet365Id(match.getBet365Id())
				.createdAt(match.getCreatedAt())
				.updatedAt(match.getUpdatedAt());
		}

		return builder.build();
	}
}
