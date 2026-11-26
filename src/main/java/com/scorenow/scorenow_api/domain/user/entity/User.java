package com.scorenow.scorenow_api.domain.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 회원번호

    @Column(nullable = false)
    private String provider; // google / kakao / apple

    @Column(nullable = false)
    private String socialId; // 소셜 로그인 고유 id

    private String name; // 이름
    private String nickname; // 닉네임
    private String profileImageUrl; // 프로필사진
    private Integer warningCnt; // 경고횟수
    private String countryCode; // 국가

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private LocalDateTime deletedAt; // 탈퇴일자
    @Column(length = 500)
    private String warningReason;
    private LocalDateTime warnedAt;
    @Column(length = 500)
    private String permanentWithdrawalReason;
    private LocalDateTime permanentlyWithdrawnAt;
    @Enumerated(EnumType.STRING)
    private UserStatus statusBeforePermanentWithdrawal;
    // UTC, so all server instances agree on the expiry regardless of their local time zone.
    private LocalDateTime banExpiresAt;
    private Long tokenVersion;

    public long currentTokenVersion() { return tokenVersion == null ? 0L : tokenVersion; }
    public void revokeTokens() { tokenVersion = currentTokenVersion() + 1; }

    public boolean expireBan(LocalDateTime nowUtc) {
        if (status != UserStatus.BANNED || banExpiresAt == null || banExpiresAt.isAfter(nowUtc)) return false;
        status = UserStatus.ACTIVE;
        banExpiresAt = null;
        return true;
    }

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdateAt; // 닉네임 마지막 업데이트일자

    public void updateLoginCountry(String country) {
        this.countryCode = country;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    // 탈퇴 처리 메서드
    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    //닉네임 저장 
    public void changeNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameUpdateAt = LocalDateTime.now();
    }


}
