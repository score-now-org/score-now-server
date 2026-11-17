package com.scorenow.scorenow_api.domain.notice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor
public class NoticeController {
    private final NoticeRepository notices;
    private final NoticeLockRepository noticeLock;
    public record Response(Long id, String title, String content, boolean ticker, Notice.Platform platform,
                           int sortOrder, String url, LocalDateTime createdAt) {
        static Response published(Notice n) { return new Response(n.getId(), n.getPublishedTitle(), n.getPublishedContent(),
                n.isPublishedTicker(), n.getPublishedPlatform(), n.getPublishedSortOrder(), n.getPublishedUrl(), n.getCreatedAt()); }
    }
    public record AdminResponse(Long id, String title, String content, boolean ticker, boolean visible, boolean published,
                                Notice.Platform platform, int sortOrder, String url, LocalDateTime createdAt) {
        static AdminResponse of(Notice n) { return new AdminResponse(n.getId(), n.getTitle(), n.getContent(), n.isTicker(),
                n.isVisible(), n.isPublished(), n.getPlatform(), n.getSortOrder(), n.getUrl(), n.getCreatedAt()); }
    }
    public record OrderRequest(@NotNull @Min(1) Integer sortOrder) { }
    public record VisibilityRequest(@NotNull Boolean visible) { }
    public record StatusResponse(boolean pending) { }
    @GetMapping("/api/v1/notices") @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> list(@RequestParam(defaultValue = "false") boolean tickerOnly,
            @RequestParam(defaultValue = "ALL") Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notices.findPublished(platforms(platform), tickerOnly, pagination(page, size)).map(Response::published));
    }
    @GetMapping("/api/v1/notices/{id}") @Transactional(readOnly = true)
    public ApiResponse<Response> detail(@PathVariable Long id, @RequestParam(defaultValue = "ALL") Notice.Platform platform) {
        return ApiResponse.success(Response.published(notices.findByIdAndPublishedTrueAndPublishedPlatformIn(id, platforms(platform))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    @GetMapping({"/admin/notices", "/api/v1/admin/notices"}) @Transactional(readOnly = true)
    public ApiResponse<Page<AdminResponse>> adminList(@RequestParam(required = false) Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable p = pagination(page, size).withSort(Sort.by("sortOrder").ascending().and(Sort.by("id").descending()));
        return ApiResponse.success((platform == null ? notices.findByDeletedFalse(p) : notices.findByDeletedFalseAndPlatform(platform, p)).map(AdminResponse::of));
    }
    @GetMapping({"/admin/notices/status", "/api/v1/admin/notices/status"}) @Transactional(readOnly = true)
    public ApiResponse<StatusResponse> status() {
        return ApiResponse.success(new StatusResponse(notices.existsByModifiedTrue()));
    }
    @PostMapping({"/admin/notices", "/api/v1/admin/notices"}) @Transactional
    public ApiResponse<AdminResponse> create(@Valid @RequestBody NoticeRequest request) {
        lockForEdit();
        Notice n = new Notice(request);
        List<Notice> ordered = new ArrayList<>(notices.findByDeletedFalseOrderBySortOrderAscIdDesc());
        ordered.add(0, n); reorder(ordered);
        return ApiResponse.success(AdminResponse.of(notices.save(n)));
    }
    @PutMapping({"/admin/notices/{id}", "/api/v1/admin/notices/{id}"}) @Transactional
    public ApiResponse<AdminResponse> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        lockForEdit();
        Notice n = requireDraft(id); n.update(request);
        if (request.sortOrder() != null) {
            List<Notice> ordered = new ArrayList<>(notices.findByDeletedFalseOrderBySortOrderAscIdDesc());
            ordered.removeIf(item -> item.getId().equals(id));
            ordered.add(Math.max(0, Math.min(request.sortOrder() - 1, ordered.size())), n); reorder(ordered);
        }
        return ApiResponse.success(AdminResponse.of(n));
    }
    @PutMapping({"/admin/notices/{id}/order", "/api/v1/admin/notices/{id}/order"}) @Transactional
    public ApiResponse<AdminResponse> move(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        lockForEdit();
        Notice n = requireDraft(id);
        List<Notice> ordered = new ArrayList<>(notices.findByDeletedFalseOrderBySortOrderAscIdDesc());
        ordered.removeIf(item -> item.getId().equals(id));
        ordered.add(Math.max(0, Math.min(request.sortOrder() - 1, ordered.size())), n);
        reorder(ordered);
        return ApiResponse.success(AdminResponse.of(n));
    }
    @PatchMapping({"/admin/notices/{id}/visibility", "/api/v1/admin/notices/{id}/visibility"}) @Transactional
    public ApiResponse<AdminResponse> visibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest request) {
        lockForEdit();
        Notice n = requireDraft(id);
        n.setVisible(request.visible());
        return ApiResponse.success(AdminResponse.of(n));
    }
    @DeleteMapping({"/admin/notices/{id}", "/api/v1/admin/notices/{id}"}) @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lockForEdit();
        requireDraft(id).remove();
        reorder(notices.findByDeletedFalseOrderBySortOrderAscIdDesc());
        return ApiResponse.success();
    }
    @PostMapping({"/admin/notices/publish", "/api/v1/admin/notices/publish"}) @Transactional
    public ApiResponse<Void> publish() {
        lockForEdit();
        List<Notice> all = notices.findAll();
        for (Notice n : all) if (!n.isDeleted()) n.publish();
        notices.deleteAll(all.stream().filter(Notice::isDeleted).toList());
        return ApiResponse.success();
    }
    private static void reorder(List<Notice> notices) {
        for (int i = 0; i < notices.size(); i++) notices.get(i).setSortOrder(i + 1);
    }
    private void lockForEdit() {
        noticeLock.lockForEdit().orElseThrow(() -> new IllegalStateException("공지 관리 DB 마이그레이션이 필요합니다."));
    }
    private Notice requireDraft(Long id) { return notices.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private static List<Notice.Platform> platforms(Notice.Platform platform) {
        return platform == Notice.Platform.ALL ? List.of(platform) : List.of(Notice.Platform.ALL, platform);
    }
    private static PageRequest pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return PageRequest.of(page, size);
    }
}
