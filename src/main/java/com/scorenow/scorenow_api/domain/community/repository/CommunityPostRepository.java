package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, JpaSpecificationExecutor<CommunityPost>, CommunityPostRepositoryCustom {
	@Override
	@EntityGraph(attributePaths = "author")
	Page<CommunityPost> findAll(Specification<CommunityPost> specification, Pageable pageable);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update CommunityPost p set p.status = :deleted, p.deletedAt = :now, p.commentCount = 0 where p.id = :id and p.status <> :deleted")
	int markDeletedByAdmin(@Param("id") Long id, @Param("deleted") CommunityPostStatus deleted, @Param("now") LocalDateTime now);

	Optional<CommunityPost> findByIdAndStatus(Long id, CommunityPostStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from CommunityPost p where p.id = :id")
	Optional<CommunityPost> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from CommunityPost p where p.id = :id and p.status = :status")
	Optional<CommunityPost> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") CommunityPostStatus status);

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
