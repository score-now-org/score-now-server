package com.scorenow.scorenow_api.domain.admin.member.dto;

import com.scorenow.scorenow_api.domain.user.entity.UserLoginHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberSecurityInfoResponseDto {

    private List<LoginHistoryDto> loginHistories;

    public static MemberSecurityInfoResponseDto from(List<UserLoginHistory> histories) {
        return MemberSecurityInfoResponseDto.builder()
                .loginHistories(histories.stream()
                        .map(h -> LoginHistoryDto.builder()
                                .lastLoginAt(h.getLoginAt())
                                .lastLoginCountryCode(h.getCountryCode())
                                .lastLoginIp(h.getIpAddress())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

}
