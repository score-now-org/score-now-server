package com.scorenow.scorenow_api.domain.chat.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.scorenow.scorenow_api.domain.chat.service.ChatReportService;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityImageUploadResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityImageService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatModerationController {
    private final ChatReportService reports;
    private final CommunityImageService images;
    public record ReportRequest(@NotBlank @Size(max = 500) String reason) { }

    @PostMapping("/messages/{id}/reports")
    public ApiResponse<Void> report(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        reports.report(id, userId, request.reason());
        return ApiResponse.success();
    }
    @PostMapping("/images")
    public ApiResponse<CommunityImageUploadResponse> upload(@AuthenticationPrincipal Long userId, @RequestPart("image") MultipartFile image) {
        return ApiResponse.success(images.uploadImage(userId, image));
    }
}
