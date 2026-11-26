package com.scorenow.scorenow_api.domain.admin.community;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {
    private final AdminCommunityService community;

    @GetMapping("/posts")
    public ApiResponse<AdminCommunityService.PostList> posts(@RequestParam(required = false) String title,
            @RequestParam(required = false) String socialId, @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Long memberId, @RequestParam(defaultValue = "false") boolean hotOnly,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(community.posts(title, socialId, nickname, memberId, hotOnly, pageable));
    }

    @GetMapping("/comments")
    public ApiResponse<Page<AdminCommunityService.CommentItem>> comments(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String socialId, @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Long memberId, @RequestParam(required = false) Long postId,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(community.comments(keyword, socialId, nickname, memberId, postId, pageable));
    }

    @GetMapping("/images")
    public ApiResponse<Page<AdminCommunityService.ImageItem>> images(@RequestParam(required = false) String socialId,
            @RequestParam(required = false) String nickname, @RequestParam(required = false) Long memberId,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(community.images(socialId, nickname, memberId, pageable));
    }

    @DeleteMapping("/images/{id}")
    public ApiResponse<Void> deleteImage(@PathVariable Long id) { community.deleteImage(id); return ApiResponse.success(); }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) { community.deletePost(id); return ApiResponse.success(); }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) { community.deleteComment(id); return ApiResponse.success(); }
}
