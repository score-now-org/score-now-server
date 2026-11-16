package com.scorenow.scorenow_api.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessageReport;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageReportRepository;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatReportService {
    private final ChatMessageRepository messages;
    private final ChatMessageReportRepository reports;
    private final UserReferenceService users;

    @Transactional
    public void report(Long messageId, Long userId, String reason) {
        if (userId == null) throw new BusinessException(ErrorCode.CHAT_SEND_AUTH_REQUIRED);
        if (!StringUtils.hasText(reason) || reason.length() > 500) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        var reporter = users.requireActiveUser(userId);
        var message = messages.findByIdForUpdate(messageId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (message.getDeletedAt() != null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (reports.existsByMessage_IdAndReporter_Id(messageId, userId)) throw new BusinessException(ErrorCode.COMMUNITY_REPORT_DUPLICATED);
        reports.save(new ChatMessageReport(message, reporter, reason.trim()));
        message.markReported();
    }
}
