package com.scorenow.scorenow_api.domain.community.repository;

import static com.scorenow.scorenow_api.domain.community.entity.QCommunityPost.communityPost;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostSort;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommunityPostRepositoryImpl implements CommunityPostRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<CommunityPost> searchActivePosts(CommunityBoardType boardType, CommunityPostSort sort, Long cursor,
		int limit) {
		return queryFactory
			.selectFrom(communityPost)
			.leftJoin(communityPost.author).fetchJoin()
			.where(
				active(),
				categoryEq(boardType),
				postCursorLt(sort, cursor)
			)
			.orderBy(postOrder(sort))
			.limit(limit)
			.fetch();
	}

	@Override
	public List<CommunityPost> searchMyPosts(Long userId, Long cursor, int limit) {
		return queryFactory
			.selectFrom(communityPost)
			.leftJoin(communityPost.author).fetchJoin()
			.where(
				active(),
				communityPost.author.id.eq(userId),
				cursorLt(cursor)
			)
			.orderBy(communityPost.id.desc())
			.limit(limit)
			.fetch();
	}

	@Override
	public List<CommunityPost> findPopularCandidates(LocalDateTime since, int limit) {
		return queryFactory
			.selectFrom(communityPost)
			.leftJoin(communityPost.author).fetchJoin()
			.where(
				active(),
				communityPost.createdAt.goe(since)
			)
			.orderBy(popularCandidateOrder())
			.limit(limit)
			.fetch();
	}

	private BooleanExpression active() {
		return communityPost.status.eq(CommunityPostStatus.ACTIVE);
	}

	private BooleanExpression cursorLt(Long cursor) {
		return cursor == null ? null : communityPost.id.lt(cursor);
	}

	private BooleanExpression postCursorLt(CommunityPostSort sort, Long cursor) {
		if (sort != CommunityPostSort.RECOMMENDED || cursor == null) {
			return cursorLt(cursor);
		}

		Long likeCount = queryFactory.select(communityPost.likeCount)
			.from(communityPost)
			.where(communityPost.id.eq(cursor))
			.fetchOne();
		if (likeCount == null) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}

		return communityPost.likeCount.lt(likeCount)
			.or(communityPost.likeCount.eq(likeCount).and(communityPost.id.lt(cursor)));
	}

	private OrderSpecifier<?>[] postOrder(CommunityPostSort sort) {
		return sort == CommunityPostSort.RECOMMENDED
			? new OrderSpecifier[] {communityPost.likeCount.desc(), communityPost.id.desc()}
			: new OrderSpecifier[] {communityPost.id.desc()};
	}

	private BooleanExpression categoryEq(CommunityBoardType boardType) {
		if (boardType == null || !boardType.isCategory()) {
			return null;
		}

		CommunityCategory category = CommunityCategory.fromBoardType(boardType);
		return communityPost.category.eq(category);
	}

	private OrderSpecifier<?>[] popularCandidateOrder() {
		NumberExpression<Long> popularScore = communityPost.viewCount.add(
			communityPost.likeCount.multiply(CommunityPost.POPULAR_LIKE_WEIGHT)
		);

		return new OrderSpecifier[] {
			popularScore.desc(),
			communityPost.id.desc()
		};
	}
}
