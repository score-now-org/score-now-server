package com.scorenow.scorenow_api.domain.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

	Optional<CommunityComment> findByIdAndStatus(Long id, CommunityCommentStatus status);

	List<CommunityComment> findByPost_IdAndStatusOrderByIdAsc(Long postId, CommunityCommentStatus status,
		Pageable pageable);

	List<CommunityComment> findByPost_IdAndStatusAndIdGreaterThanOrderByIdAsc(Long postId,
		CommunityCommentStatus status, Long cursor, Pageable pageable);
}
