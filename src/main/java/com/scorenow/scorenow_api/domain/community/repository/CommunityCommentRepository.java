package com.scorenow.scorenow_api.domain.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

	Optional<CommunityComment> findByIdAndStatus(Long id, CommunityCommentStatus status);

	@EntityGraph(attributePaths = "author")
	List<CommunityComment> findByPost_IdAndStatusOrderByIdDesc(Long postId, CommunityCommentStatus status,
		Pageable pageable);

	@EntityGraph(attributePaths = "author")
	List<CommunityComment> findByPost_IdAndStatusAndIdLessThanOrderByIdDesc(Long postId,
		CommunityCommentStatus status, Long cursor, Pageable pageable);

	@Query("""
		select c from CommunityComment c
		join fetch c.author
		where c.post.id = :postId and c.status = :status
		order by c.likeCount desc, c.id desc
		""")
	List<CommunityComment> findRecommendedComments(@Param("postId") Long postId,
		@Param("status") CommunityCommentStatus status, Pageable pageable);

	@Query("""
		select c from CommunityComment c
		join fetch c.author
		where c.post.id = :postId and c.status = :status
		and (c.likeCount < :likeCount or (c.likeCount = :likeCount and c.id < :commentId))
		order by c.likeCount desc, c.id desc
		""")
	List<CommunityComment> findRecommendedCommentsAfter(@Param("postId") Long postId,
		@Param("status") CommunityCommentStatus status, @Param("likeCount") long likeCount,
		@Param("commentId") long commentId, Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityComment c
		set c.likeCount = c.likeCount + 1
		where c.id = :commentId and c.status = :status
		""")
	int increaseLikeCount(@Param("commentId") Long commentId, @Param("status") CommunityCommentStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update CommunityComment c
		set c.likeCount = case when c.likeCount > 0 then c.likeCount - 1 else 0 end
		where c.id = :commentId and c.status = :status
		""")
	int decreaseLikeCount(@Param("commentId") Long commentId, @Param("status") CommunityCommentStatus status);
}
