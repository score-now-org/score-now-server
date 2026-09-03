package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;

public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage, Long> {

	List<CommunityPostImage> findByPost_IdOrderBySortOrderAsc(Long postId);

	List<CommunityPostImage> findByPost_IdInOrderByPost_IdAscSortOrderAsc(Collection<Long> postIds);

	void deleteByPost(CommunityPost post);
}
