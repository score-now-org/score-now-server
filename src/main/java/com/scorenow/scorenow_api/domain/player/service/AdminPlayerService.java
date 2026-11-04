package com.scorenow.scorenow_api.domain.player.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerCreateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerSquadUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerResponse;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlayerService {

	private final PlayerRepository playerRepository;
	private final TeamRepository teamRepository;
	private final LeagueRepository leagueRepository;
	private final SportRepository sportRepository;

	private final PlayerTeamDetailRepository playerTeamDetailRepository;
	private final PlayerExternalMappingRepository playerExternalMappingRepository;

	// 관리자 선수 등록
	@Transactional
	public Long createPlayer(AdminPlayerCreateRequest request) {

		// 종목 조회
		Sport sport = sportRepository.findById(request.sportId())
			.orElseThrow(() ->
				new BusinessException(ErrorCode.SPORT_NOT_FOUND)
			);

		// 팀 조회
		Team team = teamRepository.findById(request.teamId())
			.orElseThrow(() ->
				new BusinessException(ErrorCode.TEAM_NOT_FOUND)
			);

		// 팀과 선수의 종목 일치 여부
		if (!team.getSport().getId().equals(sport.getId())) {
			throw new BusinessException(
				ErrorCode.PLAYER_SPORT_MISMATCH
			);
		}

		// 리그 조회 (선택값)
		League league = null;

		if (request.leagueId() != null) {
			league = leagueRepository.findById(request.leagueId())
				.orElseThrow(() ->
					new BusinessException(ErrorCode.LEAGUE_NOT_FOUND)
				);
		}

		// Provider 검증
		DataOrigin provider = request.provider();

		if (provider == null) {
			throw new BusinessException(
				ErrorCode.PLAYER_PROVIDER_REQUIRED
			);
		}

		// API Player ID 정규화
		String apiPlayerId = request.apiPlayerId() == null
			? null
			: request.apiPlayerId().trim();

		if (apiPlayerId != null && apiPlayerId.isBlank()) {
			apiPlayerId = null;
		}

		// MANUAL은 외부 API ID를 사용하지 않음
		if (provider == DataOrigin.MANUAL) {

			if (apiPlayerId != null) {
				throw new BusinessException(
					ErrorCode.PLAYER_MANUAL_API_ID_NOT_ALLOWED
				);
			}

		} else {

			// 외부 API Provider는 API Player ID 필수
			if (apiPlayerId == null) {
				throw new BusinessException(
					ErrorCode.PLAYER_API_ID_REQUIRED
				);
			}

			// 동일 Provider + API Player ID 중복 검사
			if (playerExternalMappingRepository
				.findByProviderAndApiPlayerId(provider, apiPlayerId)
				.isPresent()) {

				throw new BusinessException(
					ErrorCode.PLAYER_API_ID_ALREADY_EXISTS
				);
			}
		}

		// 선수 생성 및 저장
		Player player = Player.builder()
			.sport(sport)
			.kName(request.kName())
			.eName(request.eName())
			.cc(request.cc())
			.birthdate(request.birthdate())
			.height(request.height())
			.taegeuk(false)
			.build();

		Player savedPlayer = playerRepository.save(player);

		// 외부 API ID가 있는 경우에만 매핑 저장
		if (apiPlayerId != null) {
			PlayerExternalMapping mapping = new PlayerExternalMapping(
				provider,
				apiPlayerId,
				savedPlayer
			);

			playerExternalMappingRepository.save(mapping);
		}

		// 선수 팀 소속 정보 저장
		PlayerTeamDetail detail = PlayerTeamDetail.builder()
			.player(savedPlayer)
			.team(team)
			.league(league)
			.shirtNumber(request.shirtNumber())
			.build();

		playerTeamDetailRepository.save(detail);

		return savedPlayer.getId();
	}

	// 스쿼드 on/off 변경
	@Transactional
	public void updateSquadOn(
		Long playerTeamDetailId,
		AdminPlayerSquadUpdateRequest request
	) {
		PlayerTeamDetail detail = playerTeamDetailRepository
			.findById(playerTeamDetailId)
			.orElseThrow(() ->
				new BusinessException(
					ErrorCode.PLAYER_TEAM_DETAIL_NOT_FOUND
				)
			);

		detail.updateSquadOn(request.squadOn());
	}

	// 선수 정보 수정 (이름, 태극)
	@Transactional
	public void updatePlayer(
		Long playerId,
		AdminPlayerUpdateRequest request
	) {
		Player player = playerRepository.findById(playerId)
			.orElseThrow(() ->
				new BusinessException(ErrorCode.PLAYER_NOT_FOUND)
			);

		if (request.kName() != null || request.eName() != null) {
			player.updateNames(
				request.kName() != null
					? request.kName()
					: player.getKName(),
				request.eName() != null
					? request.eName()
					: player.getEName()
			);
		}

		if (request.taegeuk() != null) {
			player.updateTaegeuk(request.taegeuk());
		}
	}

	// 선수 팀 소속 정보 삭제
	@Transactional
	public void deletePlayerTeamDetail(Long playerTeamDetailId) {
		PlayerTeamDetail detail = playerTeamDetailRepository
			.findById(playerTeamDetailId)
			.orElseThrow(() ->
				new BusinessException(
					ErrorCode.PLAYER_TEAM_DETAIL_NOT_FOUND
				)
			);

		playerTeamDetailRepository.delete(detail);
	}

	// 관리자 선수 검색
	public Page<AdminPlayerResponse> searchPlayers(
		Long sportId,
		Long leagueId,
		String playerKeyword,
		String teamKeyword,
		Pageable pageable
	) {
		playerKeyword = normalize(playerKeyword);
		teamKeyword = normalize(teamKeyword);

		Page<Player> players = playerRepository.searchAdminPlayers(
			sportId,
			leagueId,
			playerKeyword,
			parseId(playerKeyword),
			teamKeyword,
			parseId(teamKeyword),
			pageable
		);

		if (players.isEmpty()) {
			return Page.empty(pageable);
		}

		List<Long> playerIds = players.getContent().stream()
			.map(Player::getId)
			.toList();

		// 선수별 팀 소속 정보
		Map<Long, List<PlayerTeamDetail>> detailsByPlayerId =
			playerTeamDetailRepository.findAdminDetailsByPlayerIds(
					playerIds,
					leagueId,
					teamKeyword,
					parseId(teamKeyword)
				)
				.stream()
				.collect(Collectors.groupingBy(
					detail -> detail.getPlayer().getId()
				));

		// 선수별 외부 API 매핑 정보
		Map<Long, PlayerExternalMapping> mappingsByPlayerId =
			playerExternalMappingRepository.findAllByPlayerIds(playerIds)
				.stream()
				.collect(Collectors.toMap(
					mapping -> mapping.getPlayer().getId(),
					Function.identity(),
					(first, ignored) -> first
				));

		return players.map(player ->
			AdminPlayerResponse.from(
				player,
				detailsByPlayerId.getOrDefault(
					player.getId(),
					List.of()
				),
				mappingsByPlayerId.get(player.getId())
			)
		);
	}

	private String normalize(String keyword) {
		return keyword == null || keyword.isBlank()
			? null
			: keyword.trim();
	}

	private Long parseId(String keyword) {
		if (keyword == null || !keyword.matches("\\d+")) {
			return null;
		}

		try {
			return Long.valueOf(keyword);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}