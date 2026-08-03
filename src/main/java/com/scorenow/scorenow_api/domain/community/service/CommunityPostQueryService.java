package com.scorenow.scorenow_api.domain.community.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostDetailResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostImageResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostListResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostSliceResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostImageRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityPostQueryService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int POPULAR_TOP_MAX_SIZE = 3;
	private static final int POPULAR_ROLLING_MAX_SIZE = 5;
	private static final int POPULAR_CANDIDATE_LIMIT = 500;

	private final CommunityPostRepository postRepository;
	private final CommunityPostImageRepository imageRepository;
	private final CommunityImageService imageService;
	private final CommunityAuthService authService;

	@Transactional(readOnly = true)
	public CommunityPostSliceResponse getPosts(CommunityBoardType boardType, Long cursor, int size) {
		int normalizedSize = normalizeSize(size);
		CommunityBoardType normalizedBoardType = boardType == null ? CommunityBoardType.ALL : boardType;

		if (normalizedBoardType.isPopular()) {
			return createSlice(getPopularPosts(cursor, normalizedSize + 1), normalizedSize);
		}

		List<CommunityPost> posts = postRepository.searchActivePosts(normalizedBoardType, cursor, normalizedSize + 1);
		return createSlice(posts, normalizedSize);
	}

	@Transactional(readOnly = true)
	public CommunityPostSliceResponse getMyPosts(Long userId, Long cursor, int size) {
		Long loginUserId = authService.requireLogin(userId);
		int normalizedSize = normalizeSize(size);

		List<CommunityPost> posts = postRepository.searchMyPosts(loginUserId, cursor, normalizedSize + 1);
		return createSlice(posts, normalizedSize);
	}

	@Transactional(readOnly = true)
	public List<CommunityPostListResponse> getPopularRollingPosts(Integer size) {
		int normalizedSize = normalizePopularSize(size, POPULAR_ROLLING_MAX_SIZE);
		return toListResponses(getPopularPosts(null, normalizedSize));
	}

	@Transactional(readOnly = true)
	public List<CommunityPostListResponse> getPopularTopPosts(Integer size) {
		int normalizedSize = normalizePopularSize(size, POPULAR_TOP_MAX_SIZE);
		return toListResponses(getPopularPosts(null, normalizedSize));
	}

	@Transactional
	public CommunityPostDetailResponse getPostDetail(Long postId) {
		int updatedRows = postRepository.increaseViewCount(postId, CommunityPostStatus.ACTIVE);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}

		CommunityPost post = postRepository.findByIdAndStatus(postId, CommunityPostStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

		List<CommunityPostImageResponse> images = imageService.toResponses(
			imageRepository.findByPost_IdOrderBySortOrderAsc(postId)
		);

		return CommunityPostDetailResponse.of(post, images, LocalDateTime.now());
	}

	private CommunityPostSliceResponse createSlice(List<CommunityPost> posts, int size) {
		boolean hasNext = posts.size() > size;
		List<CommunityPost> slicedPosts = hasNext ? posts.subList(0, size) : posts;
		Long nextCursor = slicedPosts.isEmpty() ? null : slicedPosts.get(slicedPosts.size() - 1).getId();

		return CommunityPostSliceResponse.of(toListResponses(slicedPosts), nextCursor, hasNext);
	}

	private List<CommunityPostListResponse> toListResponses(List<CommunityPost> posts) {
		LocalDateTime now = LocalDateTime.now();
		Map<Long, String> thumbnailUrls = getThumbnailUrls(posts);

		return posts.stream()
			.map(post -> CommunityPostListResponse.of(post, thumbnailUrls.get(post.getId()), now))
			.toList();
	}

	private Map<Long, String> getThumbnailUrls(List<CommunityPost> posts) {
		List<Long> postIds = posts.stream()
			.map(CommunityPost::getId)
			.toList();

		if (postIds.isEmpty()) {
			return Map.of();
		}

		return imageRepository.findByPost_IdInOrderByPost_IdAscSortOrderAsc(postIds)
			.stream()
			.collect(Collectors.toMap(
				image -> image.getPost().getId(),
				image -> imageService.getImageUrl(image.getImageKey()),
				(existing, ignored) -> existing,
				LinkedHashMap::new
			));
	}

	private List<CommunityPost> getPopularPosts(Long cursor, int limit) {
		LocalDateTime now = LocalDateTime.now();
		List<CommunityPost> candidates = postRepository.findPopularCandidates(now.minusHours(24), POPULAR_CANDIDATE_LIMIT);

		if (candidates.size() < limit) {
			Map<Long, CommunityPost> merged = candidates.stream()
				.collect(Collectors.toMap(CommunityPost::getId, Function.identity(), (left, right) -> left,
					LinkedHashMap::new));
			postRepository.findPopularCandidates(now.minusDays(7), POPULAR_CANDIDATE_LIMIT)
				.forEach(post -> merged.putIfAbsent(post.getId(), post));
			candidates = List.copyOf(merged.values());
		}

		List<CommunityPost> rankedPosts = candidates.stream()
			.sorted(Comparator
				.comparingDouble((CommunityPost post) -> post.getPopularScore(now)).reversed()
				.thenComparing(CommunityPost::getId, Comparator.reverseOrder()))
			.toList();

		if (cursor == null) {
			return rankedPosts.stream().limit(limit).toList();
		}

		int cursorIndex = -1;
		for (int index = 0; index < rankedPosts.size(); index++) {
			if (rankedPosts.get(index).getId().equals(cursor)) {
				cursorIndex = index;
				break;
			}
		}

		if (cursorIndex < 0) {
			return List.of();
		}

		return rankedPosts.stream()
			.skip(cursorIndex + 1L)
			.limit(limit)
			.toList();
	}

	private int normalizeSize(int size) {
		if (size == 0) {
			return DEFAULT_SIZE;
		}

		if (size < 0 || size > MAX_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}

		return size;
	}

	private int normalizePopularSize(Integer size, int maxSize) {
		if (size == null || size == 0) {
			return maxSize;
		}

		if (size < 0) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}

		return Math.min(size, maxSize);
	}
}
