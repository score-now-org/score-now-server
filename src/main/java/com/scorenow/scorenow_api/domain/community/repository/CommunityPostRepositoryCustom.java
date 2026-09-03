package com.scorenow.scorenow_api.domain.community.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;

public interface CommunityPostRepositoryCustom {

	List<CommunityPost> searchActivePosts(CommunityBoardType boardType, Long cursor, int limit);

	List<CommunityPost> searchMyPosts(Long userId, Long cursor, int limit);

	List<CommunityPost> findPopularCandidates(LocalDateTime since, int limit);
}
