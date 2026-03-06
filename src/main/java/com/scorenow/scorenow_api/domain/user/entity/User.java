package com.scorenow.scorenow_api.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;      // google / kakao / apple

    @Column(nullable = false)
    private String socialId;     // 소셜 로그인 고유 id

    private String name;
    private String nickname;
    private String profileImageUrl;

}
