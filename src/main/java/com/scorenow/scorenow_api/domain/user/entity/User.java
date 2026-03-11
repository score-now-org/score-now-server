package com.scorenow.scorenow_api.domain.user.entity;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
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

}
