package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.player.entity.QPlayer;
import com.scorenow.scorenow_api.domain.player.entity.QPlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.QPlayerTeamDetail;
import com.scorenow.scorenow_api.domain.team.entity.QTeam;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MatchLineupSearchRepositoryImpl implements MatchLineupSearchRepository {

	private final JPAQueryFactory jpaQueryFactory;

	private static final QPlayer player = QPlayer.player;
	private static final QPlayerTeamDetail detail = QPlayerTeamDetail.playerTeamDetail;
	private static final QTeam team = QTeam.team;
	private static final QPlayerExternalMapping externalMapping = QPlayerExternalMapping.playerExternalMapping;

	@Override
	public Optional<MatchLineupPlayerResponse> findPlayerByIdAndTeamId(
		Long playerId,
		Long teamId
	) {
		MatchLineupPlayerResponse result = jpaQueryFactory
			.select(Projections.constructor(
				MatchLineupPlayerResponse.class,
				player.id,
				externalMapping.provider,
				externalMapping.apiPlayerId,
				player.kName,
				player.eName,
				detail.position,
				team.id,
				displayTeamName(),
				detail.shirtNumber,
				player.id.isNotNull()
			))
			.from(detail)
			.join(detail.player, player)
			.join(detail.team, team)
			.leftJoin(externalMapping)
			.on(externalMapping.player.eq(player))
			.where(
				player.id.eq(playerId),
				team.id.eq(teamId)
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public List<MatchLineupPlayerResponse> findPlayersByTeamId(
		Long teamId,
		int limit
	) {
		return jpaQueryFactory
			.select(Projections.constructor(
				MatchLineupPlayerResponse.class,
				player.id,
				externalMapping.provider,
				externalMapping.apiPlayerId,
				player.kName,
				player.eName,
				detail.position,
				team.id,
				displayTeamName(),
				detail.shirtNumber,
				player.id.isNotNull()
			))
			.from(detail)
			.join(detail.player, player)
			.join(detail.team, team)
			.leftJoin(externalMapping)
			.on(externalMapping.player.eq(player))
			.where(
				team.id.eq(teamId)
			)
			.limit(limit)
			.fetch();
	}

	@Override
	public List<MatchLineupPlayerResponse> searchSelectablePlayers(
		String keyword,
		int limit
	) {
		return jpaQueryFactory
			.select(Projections.constructor(
				MatchLineupPlayerResponse.class,
				player.id,
				externalMapping.provider,
				externalMapping.apiPlayerId,
				player.kName,
				player.eName,
				detail.position,
				team.id,
				displayTeamName(),
				detail.shirtNumber,
				player.id.isNotNull()
			))
			.from(detail)
			.join(detail.player, player)
			.join(detail.team, team)
			.leftJoin(externalMapping)
			.on(externalMapping.player.eq(player))
			.where(
				keywordCondition(keyword)
			)
			.limit(limit)
			.fetch();
	}

	private BooleanExpression keywordCondition(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}

		String normalizedKeyword = keyword.trim();

		return contains(player.kName, normalizedKeyword)
			.or(contains(player.eName, normalizedKeyword))
			.or(contains(team.kName, normalizedKeyword))
			.or(contains(team.eName, normalizedKeyword));
	}

	private BooleanExpression contains(StringPath path, String keyword) {
		return path.containsIgnoreCase(keyword);
	}

	private Expression<String> displayTeamName() {
		return team.kName.coalesce(team.eName);
	}
}