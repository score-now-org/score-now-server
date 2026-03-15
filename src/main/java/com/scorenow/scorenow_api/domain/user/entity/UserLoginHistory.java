package com.scorenow.scorenow_api.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_login_history")
@Getter
@NoArgsConstructor
public class UserLoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // User테이블의 id를 FK

    private String countryCode; // 접속 국가
    private String ipAddress; // 접속 IP

    private LocalDateTime loginAt; // 로그인 시각

    @Builder
    public UserLoginHistory(User user, String countryCode, String ipAddress) {
        this.user = user;
        this.countryCode = countryCode;
        this.ipAddress = ipAddress;
        this.loginAt = LocalDateTime.now();
    }
}
