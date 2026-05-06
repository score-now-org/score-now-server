package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.player.entity.QPlayer;
import com.scorenow.scorenow_api.domain.team.entity.QTeam;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MatchLineupSearchRepositoryImpl implements MatchLineupSearchRepository {

	private final JPAQueryFactory jpaQueryFactory;

	private static final QPlayer player = QPlayer.player;
	private static final QTeam team = QTeam.team;

	@Override
	public List<MatchLineupPlayerResponse> findPlayersByTeamId(
		String teamId,
		int limit
	) {
		return jpaQueryFactory
			.select(Projections.constructor(
				MatchLineupPlayerResponse.class,
				player.id,
				player.season,
				player.kName,
				player.eName,
				player.position,
				displayTeamName(),
				player.shirtnumber,
				player.id.isNotNull() // selected 자리 임시값, 서비스에서 다시 세팅
			))
			.from(player)
//			.join(team).on(player.teamId.eq(team.id))
			.where(player.teamId.eq(teamId))
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
				player.season,
				player.kName,
				player.eName,
				player.position,
				displayTeamName(),
				player.shirtnumber,
				player.id.isNotNull() // selected 자리 임시값, 서비스에서 다시 세팅
			))
			.from(player)
//			.join(team).on(player.teamId.eq(team.id))
			.where(keywordCondition(keyword))
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

	private BooleanExpression contains(com.querydsl.core.types.dsl.StringPath path, String keyword) {
		if (path == null) {
			return null;
		}
		return path.containsIgnoreCase(keyword);
	}

	private com.querydsl.core.types.Expression<String> displayTeamName() {
		return team.kName.coalesce(team.eName);
	}

}