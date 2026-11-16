package com.scorenow.scorenow_api.domain.admin.member.dto;

import com.scorenow.scorenow_api.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MemberListResponseDto {

    private Long memberId;
    private String provider;
    private String nickname;
    private String socialId;
    private String profileImageUrl;
    private Integer warningCnt;
    private String countryCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String status;
    private String warningReason;
    private LocalDateTime warnedAt;
    private String permanentWithdrawalReason;
    private LocalDateTime permanentlyWithdrawnAt;

    public static MemberListResponseDto from(User user) {
        return MemberListResponseDto.builder()
                .memberId(user.getId())
                .provider(user.getProvider())
                .nickname(user.getNickname())
                .socialId(user.getSocialId())
                .profileImageUrl(user.getProfileImageUrl())
                .warningCnt(user.getWarningCnt())
                .countryCode(user.getCountryCode())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .warningReason(user.getWarningReason())
                .warnedAt(user.getWarnedAt())
                .permanentWithdrawalReason(user.getPermanentWithdrawalReason())
                .permanentlyWithdrawnAt(user.getPermanentlyWithdrawnAt())
                .build();
    }
}
