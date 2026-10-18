package com.scorenow.scorenow_api.domain.player.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerResponse;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlayerService {

	private final PlayerRepository playerRepository;
	private final PlayerTeamDetailRepository playerTeamDetailRepository;

	public Page<AdminPlayerResponse> searchPlayers(
		Long sportId,
		Long leagueId,
		String playerKeyword,
		String teamKeyword,
		Pageable pageable
	) {
		playerKeyword = normalize(playerKeyword);
		teamKeyword = normalize(teamKeyword);

		Page<com.scorenow.scorenow_api.domain.player.entity.Player> players =
			playerRepository.searchAdminPlayers(
				sportId,
				leagueId,
				playerKeyword,
				parseId(playerKeyword),
				teamKeyword,
				parseId(teamKeyword),
				pageable
			);

		if (players.isEmpty()) {
			return players.map(player ->
				AdminPlayerResponse.from(player, List.of())
			);
		}

		List<Long> playerIds = players.getContent().stream()
			.map(player -> player.getId())
			.toList();

		Map<Long, List<PlayerTeamDetail>> detailsByPlayerId =
			playerTeamDetailRepository.findAllByPlayerIds(playerIds)
				.stream()
				.collect(Collectors.groupingBy(
					detail -> detail.getPlayer().getId()
				));

		return players.map(player ->
			AdminPlayerResponse.from(
				player,
				detailsByPlayerId.getOrDefault(
					player.getId(),
					List.of()
				)
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
