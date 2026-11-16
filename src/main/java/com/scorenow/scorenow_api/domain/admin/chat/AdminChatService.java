package com.scorenow.scorenow_api.domain.admin.chat;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class AdminChatService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public Page<AdminChatMessageResponse> getMessages(Long memberId, Long matchId, Long sportId,
            String socialId, String nickname, Pageable pageable) {
        if (pageable.getPageSize() > 100 || (memberId != null && memberId <= 0)
                || (matchId != null && matchId <= 0) || (sportId != null && sportId <= 0)
                || pageable.getSort().stream().anyMatch(order -> !order.getProperty().equals("id"))) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        return chatMessageRepository.findForAdmin(memberId, matchId, sportId, pattern(socialId), pattern(nickname), pageable)
                .map(AdminChatMessageResponse::from);
    }

    private String pattern(String value) {
        if (!StringUtils.hasText(value)) return null;
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
