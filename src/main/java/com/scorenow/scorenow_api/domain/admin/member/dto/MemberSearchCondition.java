package com.scorenow.scorenow_api.domain.admin.member.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSearchCondition {
    private String nickname;
    private String socialId;
    private Long memberId;
    private String provider;
    private String status;
    private Integer minWarningCnt;
    private Boolean excludeWithdrawn;
    private Boolean hasProfileImage;
    private String ip;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;
}
