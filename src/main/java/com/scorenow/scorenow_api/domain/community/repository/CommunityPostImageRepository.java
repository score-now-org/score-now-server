package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;

public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage, Long> {

	List<CommunityPostImage> findByPost_IdOrderBySortOrderAsc(Long postId);

	@Query("select distinct image.post.id from CommunityPostImage image where image.post.id in :postIds")
	List<Long> findPostIdsWithImages(@Param("postIds") Collection<Long> postIds);

	void deleteByPost(CommunityPost post);
}
