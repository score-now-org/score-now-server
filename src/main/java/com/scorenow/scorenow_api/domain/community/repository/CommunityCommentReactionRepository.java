package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentReaction;

public interface CommunityCommentReactionRepository extends JpaRepository<CommunityCommentReaction, Long> {

	Optional<CommunityCommentReaction> findByComment_IdAndUser_Id(Long commentId, Long userId);

	@Query("""
		select reaction.comment.id from CommunityCommentReaction reaction
		where reaction.user.id = :userId and reaction.comment.id in :commentIds
		""")
	List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);
}
