package com.scorenow.scorenow_api.domain.admin.chat;

import java.util.Locale;
import java.util.ArrayList;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageReportRepository;
import java.time.LocalDateTime;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class AdminChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReportRepository reports;
    public record ReportInfo(Long id, String reason, LocalDateTime createdAt) { }

    @Transactional(readOnly = true)
    public Page<ReportInfo> getReports(Long messageId, Pageable pageable) {
        if (pageable.getPageSize() > 100 || pageable.getSort().stream().anyMatch(order -> !order.getProperty().equals("id"))) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        if (!chatMessageRepository.existsById(messageId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return reports.findByMessage_IdOrderByIdDesc(messageId, pageable)
            .map(report -> new ReportInfo(report.getId(), report.getReason(), report.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Page<AdminChatMessageResponse> getMessages(Long memberId, Long matchId, Long sportId,
            String socialId, String nickname, boolean reportedOnly, boolean imageOnly, Pageable pageable) {
        if (pageable.getPageSize() > 100 || (memberId != null && memberId <= 0)
                || (matchId != null && matchId <= 0) || (sportId != null && sportId <= 0)
                || pageable.getSort().stream().anyMatch(order -> !order.getProperty().equals("id"))) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        String socialIdPattern = pattern(socialId);
        String nicknamePattern = pattern(nickname);
        Specification<ChatMessage> filter = (chat, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            var user = chat.join("user", JoinType.LEFT);
            if (memberId != null) predicates.add(cb.equal(user.get("id"), memberId));
            if (matchId != null) predicates.add(cb.equal(chat.get("match").get("id"), matchId));
            if (sportId != null) predicates.add(cb.equal(chat.get("match").get("sportId"), sportId));
            if (socialIdPattern != null) predicates.add(cb.like(cb.lower(user.get("socialId")), socialIdPattern, '!'));
            if (nicknamePattern != null) {
                var displayNickname = cb.coalesce(user.<String>get("nickname"), chat.<String>get("senderNickname"));
                predicates.add(cb.like(cb.lower(displayNickname), nicknamePattern, '!'));
            }
            if (reportedOnly) predicates.add(cb.gt(chat.<Long>get("reportCount"), 0L));
            if (imageOnly) {
                predicates.add(cb.isNotNull(chat.get("imageUrl")));
                predicates.add(cb.isNull(chat.get("deletedAt")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return chatMessageRepository.findAll(filter, pageable).map(AdminChatMessageResponse::from);
    }

    @Transactional
    public void delete(Long id, String reason) {
        if (!StringUtils.hasText(reason) || reason.length() > 500) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        var message = chatMessageRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        message.markDeleted(reason.trim());
    }

    @Transactional
    public void deleteImage(Long id, String reason) {
        if (!StringUtils.hasText(reason) || reason.length() > 500) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        var message = chatMessageRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        message.removeImage(reason.trim());
    }

    private String pattern(String value) {
        if (!StringUtils.hasText(value)) return null;
        return "%" + value.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
