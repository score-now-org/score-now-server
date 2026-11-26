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
    public record NewsResponse(Long id, String title, String content, Notice.Platform platform, int sortOrder,
                               String url, String titleColor, String contentColor, LocalDateTime createdAt) {
        static NewsResponse published(Notice n) { return new NewsResponse(n.getId(), n.getPublishedTitle(), n.getPublishedContent(),
                n.getPublishedPlatform(), n.getPublishedSortOrder(), n.getPublishedUrl(), n.getPublishedTitleColor(),
                n.getPublishedContentColor(), n.getCreatedAt()); }
    }
    public record AdminResponse(Long id, String title, String content, boolean ticker, boolean visible, boolean published,
                                Notice.Platform platform, int sortOrder, String url, LocalDateTime createdAt,
                                String titleColor, String contentColor) {
        static AdminResponse of(Notice n) { return new AdminResponse(n.getId(), n.getTitle(), n.getContent(), n.isTicker(),
                n.isVisible(), n.isPublished(), n.getPlatform(), n.getSortOrder(), n.getUrl(), n.getCreatedAt(),
                n.getTitleColor(), n.getContentColor()); }
    }
    public record OrderRequest(@NotNull @Min(1) Integer sortOrder) { }
    public record VisibilityRequest(@NotNull Boolean visible) { }
    public record StatusResponse(boolean pending) { }

    @GetMapping("/api/v1/notices") @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> list(@RequestParam(defaultValue = "false") boolean tickerOnly,
            @RequestParam(defaultValue = "ALL") Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notices.findPublished(Notice.Kind.NOTICE, platforms(platform), tickerOnly, pagination(page, size)).map(Response::published));
    }
    @GetMapping("/api/v1/notices/{id}") @Transactional(readOnly = true)
    public ApiResponse<Response> detail(@PathVariable Long id, @RequestParam(defaultValue = "ALL") Notice.Platform platform) {
        return ApiResponse.success(Response.published(notices.findByIdAndKindAndPublishedTrueAndPublishedPlatformIn(id, Notice.Kind.NOTICE, platforms(platform))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    @GetMapping("/api/v1/news-tickers") @Transactional(readOnly = true)
    public ApiResponse<Page<NewsResponse>> news(@RequestParam(defaultValue = "ALL") Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notices.findPublished(Notice.Kind.NEWS, platforms(platform), false, pagination(page, size)).map(NewsResponse::published));
    }

    @GetMapping({"/admin/notices", "/api/v1/admin/notices"}) @Transactional(readOnly = true)
    public ApiResponse<Page<AdminResponse>> adminList(@RequestParam(required = false) Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminList(Notice.Kind.NOTICE, platform, page, size);
    }
    @GetMapping("/api/v1/admin/news-tickers") @Transactional(readOnly = true)
    public ApiResponse<Page<AdminResponse>> adminNews(@RequestParam(required = false) Notice.Platform platform,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminList(Notice.Kind.NEWS, platform, page, size);
    }
    private ApiResponse<Page<AdminResponse>> adminList(Notice.Kind kind, Notice.Platform platform, int page, int size) {
        Pageable p = pagination(page, size).withSort(Sort.by("sortOrder").ascending().and(Sort.by("id").descending()));
        return ApiResponse.success((platform == null ? notices.findByKindAndDeletedFalse(kind, p) : notices.findByKindAndDeletedFalseAndPlatform(kind, platform, p)).map(AdminResponse::of));
    }

    @GetMapping({"/admin/notices/status", "/api/v1/admin/notices/status"}) @Transactional(readOnly = true)
    public ApiResponse<StatusResponse> status() { return status(Notice.Kind.NOTICE); }
    @GetMapping("/api/v1/admin/news-tickers/status") @Transactional(readOnly = true)
    public ApiResponse<StatusResponse> newsStatus() { return status(Notice.Kind.NEWS); }
    private ApiResponse<StatusResponse> status(Notice.Kind kind) { return ApiResponse.success(new StatusResponse(notices.existsByKindAndModifiedTrue(kind))); }

    @PostMapping({"/admin/notices", "/api/v1/admin/notices"}) @Transactional
    public ApiResponse<AdminResponse> create(@Valid @RequestBody NoticeRequest request) { return create(Notice.Kind.NOTICE, request, null); }
    @PostMapping("/api/v1/admin/news-tickers") @Transactional
    public ApiResponse<AdminResponse> createNews(@Valid @RequestBody NewsTickerRequest request) { return create(Notice.Kind.NEWS, request.asNotice(), request); }
    private ApiResponse<AdminResponse> create(Notice.Kind kind, NoticeRequest request, NewsTickerRequest news) {
        lockForEdit();
        Notice n = new Notice(request, kind);
        if (news != null) n.setColors(news.titleColor(), news.contentColor());
        List<Notice> ordered = new ArrayList<>(notices.findByKindAndDeletedFalseOrderBySortOrderAscIdDesc(kind));
        ordered.add(0, n); reorder(ordered);
        return ApiResponse.success(AdminResponse.of(notices.save(n)));
    }

    @PutMapping({"/admin/notices/{id}", "/api/v1/admin/notices/{id}"}) @Transactional
    public ApiResponse<AdminResponse> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) { return update(Notice.Kind.NOTICE, id, request, null); }
    @PutMapping("/api/v1/admin/news-tickers/{id}") @Transactional
    public ApiResponse<AdminResponse> updateNews(@PathVariable Long id, @Valid @RequestBody NewsTickerRequest request) { return update(Notice.Kind.NEWS, id, request.asNotice(), request); }
    private ApiResponse<AdminResponse> update(Notice.Kind kind, Long id, NoticeRequest request, NewsTickerRequest news) {
        lockForEdit();
        Notice n = requireDraft(kind, id); n.update(request);
        if (news != null) n.setColors(news.titleColor(), news.contentColor());
        if (request.sortOrder() != null) moveTo(kind, n, request.sortOrder());
        return ApiResponse.success(AdminResponse.of(n));
    }

    @PutMapping({"/admin/notices/{id}/order", "/api/v1/admin/notices/{id}/order"}) @Transactional
    public ApiResponse<AdminResponse> move(@PathVariable Long id, @Valid @RequestBody OrderRequest request) { return move(Notice.Kind.NOTICE, id, request); }
    @PutMapping("/api/v1/admin/news-tickers/{id}/order") @Transactional
    public ApiResponse<AdminResponse> moveNews(@PathVariable Long id, @Valid @RequestBody OrderRequest request) { return move(Notice.Kind.NEWS, id, request); }
    private ApiResponse<AdminResponse> move(Notice.Kind kind, Long id, OrderRequest request) {
        lockForEdit(); Notice n = requireDraft(kind, id); moveTo(kind, n, request.sortOrder()); return ApiResponse.success(AdminResponse.of(n));
    }
    private void moveTo(Notice.Kind kind, Notice n, int position) {
        List<Notice> ordered = new ArrayList<>(notices.findByKindAndDeletedFalseOrderBySortOrderAscIdDesc(kind));
        ordered.removeIf(item -> item.getId().equals(n.getId()));
        ordered.add(Math.max(0, Math.min(position - 1, ordered.size())), n); reorder(ordered);
    }

    @PatchMapping({"/admin/notices/{id}/visibility", "/api/v1/admin/notices/{id}/visibility"}) @Transactional
    public ApiResponse<AdminResponse> visibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest request) { return visibility(Notice.Kind.NOTICE, id, request); }
    @PatchMapping("/api/v1/admin/news-tickers/{id}/visibility") @Transactional
    public ApiResponse<AdminResponse> newsVisibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest request) { return visibility(Notice.Kind.NEWS, id, request); }
    private ApiResponse<AdminResponse> visibility(Notice.Kind kind, Long id, VisibilityRequest request) {
        lockForEdit(); Notice n = requireDraft(kind, id); n.setVisible(request.visible()); return ApiResponse.success(AdminResponse.of(n));
    }

    @DeleteMapping({"/admin/notices/{id}", "/api/v1/admin/notices/{id}"}) @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) { return delete(Notice.Kind.NOTICE, id); }
    @DeleteMapping("/api/v1/admin/news-tickers/{id}") @Transactional
    public ApiResponse<Void> deleteNews(@PathVariable Long id) { return delete(Notice.Kind.NEWS, id); }
    private ApiResponse<Void> delete(Notice.Kind kind, Long id) {
        lockForEdit(); requireDraft(kind, id).remove(); reorder(notices.findByKindAndDeletedFalseOrderBySortOrderAscIdDesc(kind)); return ApiResponse.success();
    }

    @PostMapping({"/admin/notices/publish", "/api/v1/admin/notices/publish"}) @Transactional
    public ApiResponse<Void> publish() { return publish(Notice.Kind.NOTICE); }
    @PostMapping("/api/v1/admin/news-tickers/publish") @Transactional
    public ApiResponse<Void> publishNews() { return publish(Notice.Kind.NEWS); }
    private ApiResponse<Void> publish(Notice.Kind kind) {
        lockForEdit(); List<Notice> all = notices.findByKind(kind);
        for (Notice n : all) if (!n.isDeleted()) n.publish();
        notices.deleteAll(all.stream().filter(Notice::isDeleted).toList()); return ApiResponse.success();
    }
    private static void reorder(List<Notice> notices) { for (int i = 0; i < notices.size(); i++) notices.get(i).setSortOrder(i + 1); }
    private void lockForEdit() { noticeLock.lockForEdit().orElseThrow(() -> new IllegalStateException("공지 관리 DB 마이그레이션이 필요합니다.")); }
    private Notice requireDraft(Notice.Kind kind, Long id) { return notices.findByIdAndKindAndDeletedFalse(id, kind).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private static List<Notice.Platform> platforms(Notice.Platform platform) { return platform == null || platform == Notice.Platform.ALL ? List.of(Notice.Platform.ALL) : List.of(Notice.Platform.ALL, platform); }
    private static PageRequest pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return PageRequest.of(page, size);
    }
}
