package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, CommunityPostRepositoryCustom {

	Optional<CommunityPost> findByIdAndStatus(Long id, CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.viewCount = p.viewCount + 1
		where p.id = :postId and p.status = :status
		""")
	int increaseViewCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.likeCount = p.likeCount + 1
		where p.id = :postId and p.status = :status
		""")
	int increaseLikeCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.likeCount = case when p.likeCount > 0 then p.likeCount - 1 else 0 end
		where p.id = :postId and p.status = :status
		""")
	int decreaseLikeCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.dislikeCount = p.dislikeCount + 1
		where p.id = :postId and p.status = :status
		""")
	int increaseDislikeCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.dislikeCount = case when p.dislikeCount > 0 then p.dislikeCount - 1 else 0 end
		where p.id = :postId and p.status = :status
		""")
	int decreaseDislikeCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.commentCount = p.commentCount + 1
		where p.id = :postId and p.status = :status
		""")
	int increaseCommentCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.commentCount = case when p.commentCount > 0 then p.commentCount - 1 else 0 end
		where p.id = :postId and p.status = :status
		""")
	int decreaseCommentCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityPost p
		set p.reportCount = p.reportCount + 1
		where p.id = :postId and p.status = :status
		""")
	int increaseReportCount(@Param("postId") Long postId, @Param("status") CommunityPostStatus status);
}
