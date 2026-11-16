package com.scorenow.scorenow_api.domain.admin.community;

import java.time.*;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scorenow.scorenow_api.domain.community.entity.*;
import com.scorenow.scorenow_api.domain.community.repository.*;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {
    private static final long HOT_LIKE_COUNT = 10;
    private final CommunityPostRepository posts;
    private final CommunityCommentRepository comments;
    private final CommunityPostImageRepository images;
    private final FileStorage storage;

    public record PostItem(Long id, Long memberId, String socialId, String nickname, Integer warningCount,
            String imageUrl, String title, String content, long likeCount, long commentCount, String status, LocalDateTime createdAt) { }
    public record CommentItem(Long id, Long postId, Long memberId, String socialId, String nickname, Integer warningCount,
            String imageUrl, String postTitle, String content, long likeCount, String status, String postStatus, LocalDateTime createdAt) { }
    public record PostList(Page<PostItem> page, long todayCount) { }

    @Transactional(readOnly = true)
    public PostList posts(String title, String socialId, String nickname, Long memberId, boolean hotOnly, Pageable pageable) {
        validate(pageable, title, socialId, nickname, memberId);
        String titlePattern = pattern(title), socialPattern = pattern(socialId), nicknamePattern = pattern(nickname);
        Specification<CommunityPost> spec = (post, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            var author = post.join("author");
            if (titlePattern != null) predicates.add(cb.like(cb.lower(post.get("title")), titlePattern, '!'));
            if (socialPattern != null) predicates.add(cb.like(cb.lower(author.get("socialId")), socialPattern, '!'));
            if (nicknamePattern != null) predicates.add(cb.like(cb.lower(displayNickname(cb, author.get("nickname"), post.get("authorNickname"))), nicknamePattern, '!'));
            if (memberId != null) predicates.add(cb.equal(author.get("id"), memberId));
            if (hotOnly) { predicates.add(cb.ge(post.get("likeCount"), HOT_LIKE_COUNT)); predicates.add(cb.equal(post.get("status"), CommunityPostStatus.ACTIVE)); }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<CommunityPost> page = posts.findAll(spec, pageable);
        Map<Long, String> imageUrls = imageUrls(page.getContent().stream().map(CommunityPost::getId).toList());
        LocalDateTime[] bounds = seoulDayBounds(Instant.now(), ZoneId.systemDefault());
        long todayCount = posts.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(bounds[0], bounds[1]);
        return new PostList(page.map(p -> new PostItem(p.getId(), p.getAuthor().getId(), p.getAuthor().getSocialId(),
                nickname(p.getAuthor(), p.getAuthorNickname()), p.getAuthor().getWarningCnt(), imageUrls.get(p.getId()),
                p.getTitle(), p.getContent(), p.getLikeCount(), p.getCommentCount(), p.getStatus().name(), p.getCreatedAt())), todayCount);
    }

    @Transactional(readOnly = true)
    public Page<CommentItem> comments(String keyword, String socialId, String nickname, Long memberId, Long postId, Pageable pageable) {
        validate(pageable, keyword, socialId, nickname, memberId);
        if (postId != null && postId <= 0) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String keywordPattern = pattern(keyword), socialPattern = pattern(socialId), nicknamePattern = pattern(nickname);
        Specification<CommunityComment> spec = (comment, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            var author = comment.join("author");
            if (keywordPattern != null) predicates.add(cb.or(cb.like(cb.lower(comment.get("content").as(String.class)), keywordPattern, '!'),
                    cb.like(cb.lower(comment.get("post").get("title")), keywordPattern, '!')));
            if (socialPattern != null) predicates.add(cb.like(cb.lower(author.get("socialId")), socialPattern, '!'));
            if (nicknamePattern != null) predicates.add(cb.like(cb.lower(displayNickname(cb, author.get("nickname"), comment.get("authorNickname"))), nicknamePattern, '!'));
            if (memberId != null) predicates.add(cb.equal(author.get("id"), memberId));
            if (postId != null) predicates.add(cb.equal(comment.get("post").get("id"), postId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<CommunityComment> page = comments.findAll(spec, pageable);
        Map<Long, String> imageUrls = imageUrls(page.getContent().stream().map(CommunityComment::getPostId).distinct().toList());
        return page.map(c -> new CommentItem(c.getId(), c.getPostId(), c.getAuthorId(), c.getAuthor().getSocialId(),
                nickname(c.getAuthor(), c.getAuthorNickname()), c.getAuthor().getWarningCnt(), imageUrls.get(c.getPostId()),
                c.getPost().getTitle(), c.getContent(), c.getLikeCount(), c.getStatus().name(), c.getPost().getStatus().name(), c.getCreatedAt()));
    }

    @Transactional
    public void deletePost(Long id) {
        LocalDateTime now = LocalDateTime.now();
        if (posts.markDeletedByAdmin(id, CommunityPostStatus.DELETED, now) == 0)
            throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        comments.markPostCommentsDeleted(id, CommunityCommentStatus.DELETED, now);
    }

    @Transactional
    public void deleteComment(Long id) {
        Long postId = comments.findPostIdById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        posts.findByIdForUpdate(postId).orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
        if (comments.markDeletedIfActive(id, CommunityCommentStatus.ACTIVE, CommunityCommentStatus.DELETED, LocalDateTime.now()) == 0)
            throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
        posts.decreaseCommentCount(postId, CommunityPostStatus.ACTIVE);
    }

    private Map<Long, String> imageUrls(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, String> urls = new HashMap<>();
        for (CommunityPostImage image : images.findFirstImagesByPostIds(ids)) {
            Long postId = image.getPost().getId();
            if (!urls.containsKey(postId)) urls.put(postId, storage.getFileUrl(image.getImageKey()));
        }
        return urls;
    }
    private String nickname(User user, String snapshot) { return StringUtils.hasText(user.getNickname()) ? user.getNickname() : snapshot; }
    private Expression<String> displayNickname(CriteriaBuilder cb, Expression<String> nickname, Expression<String> snapshot) {
        return cb.<String>selectCase().when(cb.or(cb.isNull(nickname), cb.equal(cb.trim(nickname), "")), snapshot).otherwise(nickname);
    }
    static LocalDateTime[] seoulDayBounds(Instant now, ZoneId storageZone) {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime start = now.atZone(seoul).toLocalDate().atStartOfDay(seoul);
        return new LocalDateTime[]{start.withZoneSameInstant(storageZone).toLocalDateTime(),
                start.plusDays(1).withZoneSameInstant(storageZone).toLocalDateTime()};
    }
    private void validate(Pageable page, String keyword, String socialId, String nickname, Long memberId) {
        if (page.getPageSize() > 100 || (memberId != null && memberId <= 0)
                || page.getSort().stream().anyMatch(order -> !order.getProperty().equals("id"))
                || List.of(keyword == null ? "" : keyword, socialId == null ? "" : socialId, nickname == null ? "" : nickname)
                    .stream().anyMatch(value -> value.length() > 100)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    private String pattern(String value) {
        if (!StringUtils.hasText(value)) return null;
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
