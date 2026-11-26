package com.scorenow.scorenow_api.domain.admin.member.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scorenow.scorenow_api.domain.admin.member.dto.*;
import com.scorenow.scorenow_api.domain.admin.member.entity.MemberModerationEvent;
import com.scorenow.scorenow_api.domain.admin.member.repository.MemberModerationEventRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberModerationService {
    private final UserRepository users;
    private final MemberModerationEventRepository events;

    @Transactional(readOnly = true)
    public List<MemberModerationEventDto> history(Long id) {
        if (!users.existsById(id)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return events.findByUserIdOrderByIdDesc(id).stream().map(MemberModerationEventDto::from).toList();
    }

    @Transactional
    public MemberBasicInfoResponseDto edit(Long id, MemberEditRequest request) {
        User user = lockedUser(id);
        user.setName(request.name() == null ? null : request.name().trim());
        user.setCountryCode(request.countryCode() == null ? null : request.countryCode().trim().toUpperCase());
        return MemberBasicInfoResponseDto.from(user);
    }

    @Transactional
    public MemberBasicInfoResponseDto act(Long id, MemberActionRequest request) {
        User user = lockedUser(id);
        String action = request.action().trim().toUpperCase(java.util.Locale.ROOT);
        String reason = request.reason() == null ? null : request.reason().trim();
        SuspensionDuration duration = request.duration();
        if (action.equals("BAN_WEEK") || action.equals("BAN_MONTH")) {
            if (duration != null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            duration = action.equals("BAN_WEEK") ? SuspensionDuration.WEEK : SuspensionDuration.MONTH;
            action = "BAN";
        }
        if (!action.equals("RESTORE") && !StringUtils.hasText(reason))
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "사유를 입력해 주세요.");
        if (duration != null && !action.equals("BAN")) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        boolean alreadyExpired = user.expireBan(nowUtc);
        UserStatus status = user.getStatus();
        switch (action) {
            case "WARN" -> {
                if (status != UserStatus.ACTIVE && status != UserStatus.BANNED) invalidState();
                user.setWarningCnt((user.getWarningCnt() == null ? 0 : user.getWarningCnt()) + 1);
                user.setWarningReason(reason);
                user.setWarnedAt(LocalDateTime.now());
            }
            case "BAN" -> {
                if (status != UserStatus.ACTIVE) invalidState();
                user.setStatus(UserStatus.BANNED);
                user.setBanExpiresAt(duration == null ? null : duration.expiresAt(nowUtc));
                user.revokeTokens();
            }
            case "PERMANENT_WITHDRAWAL" -> {
                if (status != UserStatus.ACTIVE && status != UserStatus.BANNED) invalidState();
                user.setStatusBeforePermanentWithdrawal(status);
                user.setStatus(UserStatus.PERMANENTLY_WITHDRAWN);
                user.setPermanentWithdrawalReason(reason);
                user.setPermanentlyWithdrawnAt(LocalDateTime.now());
                user.revokeTokens();
            }
            case "DELETE_PROFILE_IMAGE" -> {
                if (!StringUtils.hasText(user.getProfileImageUrl())) invalidState();
                user.setProfileImageUrl(null);
            }
            case "RESTORE" -> {
                if (alreadyExpired) { /* Expiry raced with the administrator's manual restore. */ }
                else if (status == UserStatus.BANNED) { user.setStatus(UserStatus.ACTIVE); user.setBanExpiresAt(null); }
                else if (status == UserStatus.PERMANENTLY_WITHDRAWN) {
                    user.setStatus(user.getStatusBeforePermanentWithdrawal() == UserStatus.BANNED ? UserStatus.BANNED : UserStatus.ACTIVE);
                    user.setStatusBeforePermanentWithdrawal(null);
                    user.expireBan(nowUtc);
                } else invalidState();
            }
            default -> throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        events.save(new MemberModerationEvent(user, action, reason, duration));
        return MemberBasicInfoResponseDto.from(user);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireTimedBans() {
        users.expireBans(UserStatus.ACTIVE, UserStatus.BANNED, LocalDateTime.now(ZoneOffset.UTC));
    }

    private User lockedUser(Long id) {
        return users.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    private void invalidState() { throw new BusinessException(ErrorCode.INVALID_PARAMETER, "현재 상태에서는 처리할 수 없습니다."); }
}
