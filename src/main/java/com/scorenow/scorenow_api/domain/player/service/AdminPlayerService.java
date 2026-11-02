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

import jakarta.persistence.EntityNotFoundException;
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

	// 선수 수동 등록
	@Transactional
	public Long createPlayer(AdminPlayerCreateRequest request) {

		// 종목 조회
		Sport sport = sportRepository.findById(request.sportId())
			.orElseThrow(() ->
				new EntityNotFoundException("종목을 찾을 수 없습니다.")
			);

		// 팀 조회
		Team team = teamRepository.findById(request.teamId())
			.orElseThrow(() ->
				new EntityNotFoundException("팀을 찾을 수 없습니다.")
			);

		// 팀과 선수의 종목 일치 여부
		if (!team.getSport().getId().equals(sport.getId())) {
			throw new IllegalArgumentException("선수와 팀의 종목이 일치하지 않습니다.");
		}

		// 리그 조회 (선택값)
		League league = null;

		if (request.leagueId() != null) {
			league = leagueRepository.findById(request.leagueId())
				.orElseThrow(() ->
					new EntityNotFoundException("리그를 찾을 수 없습니다.")
				);
		}

		// Provider 및 외부 API ID 검증
		DataOrigin provider = request.provider() != null
			? request.provider()
			: DataOrigin.MANUAL;

		String apiPlayerId = request.apiPlayerId() == null
			? null
			: request.apiPlayerId().trim();

		if (provider == DataOrigin.BETS) {
			if (apiPlayerId == null || apiPlayerId.isBlank()) {
				throw new IllegalArgumentException(
					"BETS 선수는 API Player ID를 입력해야 합니다."
				);
			}

			// 동일한 외부 API 선수의 중복 등록 방지
			if (playerExternalMappingRepository
				.findByProviderAndApiPlayerId(provider, apiPlayerId)
				.isPresent()) {
				throw new IllegalArgumentException(
					"이미 등록된 BETS 선수 ID입니다."
				);
			}
		} else if (provider == DataOrigin.MANUAL) {
			if (apiPlayerId != null && !apiPlayerId.isBlank()) {
				throw new IllegalArgumentException(
					"MANUAL 선수는 API Player ID를 입력할 수 없습니다."
				);
			}
			apiPlayerId = null;
		} else {
			throw new IllegalArgumentException(
				"지원하지 않는 Provider입니다."
			);
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

		// 외부 매핑 정보 저장
		PlayerExternalMapping mapping = new PlayerExternalMapping(
			provider,
			apiPlayerId,
			savedPlayer
		);

		playerExternalMappingRepository.save(mapping);

		// 선수 팀 소속 정보 저장
		PlayerTeamDetail detail = PlayerTeamDetail.builder()
			.player(savedPlayer)
			.team(team)
			.league(league)
			.shirtNumber(request.shirtNumber())
			.build();

		playerTeamDetailRepository.save(detail);

		// 생성된 내부 선수 ID 반환
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
				new EntityNotFoundException("선수 팀 정보를 찾을 수 없습니다.")
			);

		detail.updateSquadOn(request.squadOn());
	}

	// 선수 정보 수정(이름, 태극)
	@Transactional
	public void updatePlayer(Long playerId, AdminPlayerUpdateRequest request) {
		Player player = playerRepository.findById(playerId)
			.orElseThrow(() -> new EntityNotFoundException("선수를 찾을 수 없습니다."));

		if (request.kName() != null || request.eName() != null) {
			player.updateNames(
				request.kName() != null ? request.kName() : player.getKName(),
				request.eName() != null ? request.eName() : player.getEName()
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
				new EntityNotFoundException("선수 팀 정보를 찾을 수 없습니다.")
			);

		playerTeamDetailRepository.delete(detail);
	}

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
