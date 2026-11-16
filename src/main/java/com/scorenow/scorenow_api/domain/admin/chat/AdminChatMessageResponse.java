package com.scorenow.scorenow_api.domain.admin.chat;

import java.time.LocalDateTime;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.user.entity.User;

public record AdminChatMessageResponse(Long chatId, Long matchId, Long sportId, Long memberId,
        String socialId, String nickname, String profileImageUrl, Integer warningCnt,
        String countryCode, String message, String imageUrl, Long reportCount, boolean deleted, String deletionReason, LocalDateTime createdAt) {
    static AdminChatMessageResponse from(ChatMessage chat) {
        User user = chat.getUser();
        return new AdminChatMessageResponse(chat.getId(), chat.getMatchId(), chat.getMatch().getSportId(),
                chat.getUserId(), user == null ? null : user.getSocialId(),
                user == null || user.getNickname() == null ? chat.getSenderNickname() : user.getNickname(),
                user == null ? null : user.getProfileImageUrl(),
                user == null ? null : user.getWarningCnt(), user == null ? null : user.getCountryCode(),
                chat.getMessage(), chat.getImageUrl(), chat.getReportCount() == null ? 0L : chat.getReportCount(),
                chat.getDeletedAt() != null, chat.getDeletionReason(), chat.getCreatedAt());
    }
}
