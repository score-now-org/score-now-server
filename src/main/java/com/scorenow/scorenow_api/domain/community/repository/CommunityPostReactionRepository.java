package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPostReaction;

public interface CommunityPostReactionRepository extends JpaRepository<CommunityPostReaction, Long> {

	Optional<CommunityPostReaction> findByPost_IdAndUser_Id(Long postId, Long userId);
}
