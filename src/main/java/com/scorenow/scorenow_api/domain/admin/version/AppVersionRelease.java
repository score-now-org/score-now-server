package com.scorenow.scorenow_api.domain.admin.version;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_version_releases", indexes = @Index(name = "idx_app_version_release_date", columnList = "release_date,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersionRelease {
    public enum Platform { ALL, IOS, ANDROID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_name", nullable = false, length = 40)
    private String versionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false, length = 3000)
    private String content;

    AppVersionRelease(String versionName, Platform platform, LocalDate releaseDate, String content) {
        update(versionName, platform, releaseDate, content);
    }

    void update(String versionName, Platform platform, LocalDate releaseDate, String content) {
        this.versionName = versionName;
        this.platform = platform;
        this.releaseDate = releaseDate;
        this.content = content;
    }
}
