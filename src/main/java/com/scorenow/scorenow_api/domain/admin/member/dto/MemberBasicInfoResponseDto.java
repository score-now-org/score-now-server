package com.scorenow.scorenow_api.domain.admin.member.dto;

import com.scorenow.scorenow_api.domain.user.entity.User;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBasicInfoResponseDto extends MemberListResponseDto {
    private String name;


    public static MemberBasicInfoResponseDto from(User user) {
        return MemberBasicInfoResponseDto.builder()
                //부모클래스 빌드
                .memberId(user.getId())
                .provider(user.getProvider())
                .nickname(user.getNickname())
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
                .banExpiresAt(user.getBanExpiresAt() == null ? null : user.getBanExpiresAt().toInstant(ZoneOffset.UTC))
                //자식클래스 빌드
                .profileImageUrl(user.getProfileImageUrl())
                .name(user.getName())
                .socialId(user.getSocialId())
                .build();
    }
}
