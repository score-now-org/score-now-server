package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentReaction;

public interface CommunityCommentReactionRepository extends JpaRepository<CommunityCommentReaction, Long> {

	Optional<CommunityCommentReaction> findByComment_IdAndUser_Id(Long commentId, Long userId);
}
