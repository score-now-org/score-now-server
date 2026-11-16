package com.scorenow.scorenow_api.domain.admin.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/chat/messages")
@RequiredArgsConstructor
public class AdminChatController {
    private final AdminChatService adminChatService;

    @GetMapping
    public ApiResponse<Page<AdminChatMessageResponse>> getMessages(
            @RequestParam(required = false) Long memberId, @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Long sportId, @RequestParam(required = false) String socialId,
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "false") boolean reportedOnly,
            @RequestParam(defaultValue = "false") boolean imageOnly,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(adminChatService.getMessages(memberId, matchId, sportId, socialId, nickname, reportedOnly, imageOnly, pageable));
    }
    @GetMapping("/{id}/reports")
    public ApiResponse<Page<AdminChatService.ReportInfo>> reports(@PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(adminChatService.getReports(id, pageable));
    }

    public record DeleteRequest(@NotBlank @Size(max = 500) String reason) { }

    @PostMapping("/{id}/image/delete")
    public ApiResponse<Void> deleteImage(@PathVariable Long id, @Valid @RequestBody DeleteRequest request) {
        adminChatService.deleteImage(id, request.reason());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id, @Valid @RequestBody DeleteRequest request) {
        adminChatService.delete(id, request.reason());
        return ApiResponse.success();
    }
}
