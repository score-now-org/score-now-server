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
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommunityPostRepositoryImpl implements CommunityPostRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<CommunityPost> searchActivePosts(CommunityBoardType boardType, Long cursor, int limit) {
		return queryFactory
			.selectFrom(communityPost)
			.leftJoin(communityPost.author).fetchJoin()
			.where(
				active(),
				categoryEq(boardType),
				cursorLt(cursor)
			)
			.orderBy(communityPost.id.desc())
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

	private BooleanExpression categoryEq(CommunityBoardType boardType) {
		if (boardType == null || !boardType.isCategory()) {
			return null;
		}

		CommunityCategory category = CommunityCategory.fromBoardType(boardType);
		return communityPost.category.eq(category);
	}

	private OrderSpecifier<?>[] popularCandidateOrder() {
		NumberExpression<Long> recommendationCount = communityPost.likeCount.add(communityPost.dislikeCount);
		NumberExpression<Long> popularScore = communityPost.viewCount.add(
			recommendationCount.multiply(CommunityPost.POPULAR_RECOMMENDATION_WEIGHT)
		);

		return new OrderSpecifier[] {
			popularScore.desc(),
			communityPost.id.desc()
		};
	}
}
