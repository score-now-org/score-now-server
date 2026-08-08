package com.scorenow.scorenow_api.domain.match.repository.jpa;

import static com.scorenow.scorenow_api.domain.league.entity.QLeague.*;
import static com.scorenow.scorenow_api.domain.match.entity.QMatch.*;
import static com.scorenow.scorenow_api.domain.sport.entity.QSport.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryImpl implements MatchRepositoryCustom{

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<Match> searchMatches(MatchSearchCondition condition, Pageable pageable){
		List<Match> content = queryFactory
			.selectFrom(match)
			.leftJoin(match.league, league).fetchJoin()
			.leftJoin(match.sport, sport).fetchJoin()
			.leftJoin(match.homeTeam).fetchJoin()
			.leftJoin(match.awayTeam).fetchJoin()
			.where(
				sportIdEq(condition.getSportId()),
				leagueIdEq(condition.getLeagueId()),
				leagueNameContains(condition.getLeagueName()),
				statusEq(condition.getStatus()),
				isActiveEq(condition.getIsActive()),
				isManualEq(condition.getIsManual()),
				dateBetween(condition.getDate())
			)
			.orderBy(createOrderSpecifier(condition.getSortBy(), condition.getSortDirection()))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = queryFactory
			.select(match.count())
			.from(match)
			.leftJoin(league).on(match.leagueId.eq(league.id))
			.where(
				sportIdEq(condition.getSportId()),
				leagueIdEq(condition.getLeagueId()),
				leagueNameContains(condition.getLeagueName()),
				statusEq(condition.getStatus()),
				isActiveEq(condition.getIsActive()),
				isManualEq(condition.getIsManual()),
				dateBetween(condition.getDate())
			)
			.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total: 0L);
	}

	@Override
	public Optional<Match> findByIdWithRelations(Long matchId){
		Match result = queryFactory
			.selectFrom(match)
			.leftJoin(match.sport, sport).fetchJoin()
			.leftJoin(match.league, league).fetchJoin()
			.leftJoin(match.homeTeam).fetchJoin()
			.leftJoin(match.awayTeam).fetchJoin()
			.where(match.id.eq(matchId))
			.fetchOne();

		return Optional.ofNullable(result);
	}


	// === Private Helper Methods ===

	private BooleanExpression sportIdEq(Long sportId){
		return sportId != null ? match.sportId.eq(sportId) : null;
	}

	private BooleanExpression leagueIdEq(Long leagueId){
		return leagueId != null ? match.leagueId.eq(leagueId) : null;
	}

	private BooleanExpression leagueNameContains(String leagueName){
		if(!StringUtils.hasText(leagueName)) return null;
		return league.kName.contains(leagueName)
			.or(league.eName.contains(leagueName))
			.or(league.sName.contains(leagueName));
	}

	private BooleanExpression statusEq(String status){
		if(status == null || status.isBlank()) return null;
		try{
			return match.statusCode.eq(MatchStatus.valueOf(status.toUpperCase()));
		}catch (Exception e){
			return null;
		}
	}

	private BooleanExpression isActiveEq(Boolean isActive){
		return isActive != null ? match.isActive.eq(isActive): null;
	}

	private BooleanExpression isManualEq(Boolean isManual){
		return isManual != null ? match.isManual.eq(isManual): null;
	}

	private BooleanExpression dateBetween(String date){
		if(date == null || date.isBlank()) return null;

		try {
			LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
			LocalDateTime startOfDay = localDate.atStartOfDay();
			LocalDateTime endOfDay = localDate.atTime(LocalTime.MAX);

			return match.startAt.between(startOfDay, endOfDay);
		} catch (java.time.format.DateTimeParseException e) {
			return null;
		}
	}

	private OrderSpecifier<?> createOrderSpecifier(String sortBy, String sortDirection){
		Order order = "DESC".equalsIgnoreCase(sortDirection) ? Order.DESC : Order.ASC;

		// sortBy가 null이면 기본값: 날짜 오름차순
		if(sortBy == null || sortBy.isBlank()){
			return new OrderSpecifier<>(Order.ASC, match.startAt);
		}

		switch(sortBy.toLowerCase()){
			case "sport":
				return new OrderSpecifier<>(order, match.sportId);
			case "league":
				return new OrderSpecifier<>(order, league.kName);
			case "status":
				return new OrderSpecifier<>(order, match.statusCode);
			case "date":
				return new OrderSpecifier<>(order, match.startAt);
			default:
				return new OrderSpecifier<>(Order.ASC, match.startAt);
		}
	}
}
