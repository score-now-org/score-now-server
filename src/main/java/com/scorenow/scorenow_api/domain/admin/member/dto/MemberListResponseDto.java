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
    private Integer warningCnt;
    private String countryCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;

    public static MemberListResponseDto from(User user) {
        return MemberListResponseDto.builder()
                .memberId(user.getId())
                .provider(user.getProvider())
                .nickname(user.getNickname())
                .warningCnt(user.getWarningCnt())
                .countryCode(user.getCountryCode())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .status(String.valueOf(user.getStatus()))
                .build();
    }
}
