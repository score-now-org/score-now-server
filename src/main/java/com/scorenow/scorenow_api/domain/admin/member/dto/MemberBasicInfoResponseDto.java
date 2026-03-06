package com.scorenow.scorenow_api.domain.admin.member.dto;

import com.scorenow.scorenow_api.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemberBasicInfoResponseDto extends MemberListResponseDto {
    private String profileImageUrl;
    private String socialId;
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
                .status(String.valueOf(user.getStatus()))
                //자식클래스 빌드
                .profileImageUrl(user.getProfileImageUrl())
                .name(user.getName())
                .socialId(user.getSocialId())
                .build();
    }
}
