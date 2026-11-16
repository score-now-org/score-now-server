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
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(adminChatService.getMessages(memberId, matchId, sportId, socialId, nickname, pageable));
    }
}
