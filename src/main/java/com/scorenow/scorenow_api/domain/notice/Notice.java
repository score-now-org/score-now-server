package com.scorenow.scorenow_api.domain.notice;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "notices") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {
    public enum Platform { ALL, ANDROID, IOS }
    public enum Kind { NOTICE, NEWS }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Kind kind = Kind.NOTICE;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false) private boolean ticker;
    @Column(nullable = false) private boolean visible;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Platform platform;
    @Column(nullable = false) private int sortOrder;
    @Column(length = 2048) private String url;
    @Column(nullable = false) private boolean deleted;
    @Column(nullable = false) private boolean modified;
    @Column(nullable = false) private boolean published;
    @Column(length = 200) private String publishedTitle;
    @Lob @Column(columnDefinition = "TEXT") private String publishedContent;
    private boolean publishedTicker;
    @Enumerated(EnumType.STRING) @Column(length = 10) private Platform publishedPlatform;
    private int publishedSortOrder;
    @Column(length = 2048) private String publishedUrl;
    @Column(length = 7) private String titleColor;
    @Column(length = 7) private String contentColor;
    @Column(length = 7) private String publishedTitleColor;
    @Column(length = 7) private String publishedContentColor;

    public Notice(NoticeRequest request) { update(request); }
    public Notice(NoticeRequest request, Kind kind) { this.kind = kind; update(request); }

    public void update(NoticeRequest request) {
        title = request.title().trim(); content = request.content().trim(); ticker = request.ticker();
        visible = request.visible(); platform = request.platform(); url = request.url();
        modified = true;
    }
    public void setSortOrder(int order) { if (sortOrder != order) { sortOrder = order; modified = true; } }
    public void setVisible(boolean value) { if (visible != value) { visible = value; modified = true; } }
    public void setColors(String titleColor, String contentColor) {
        this.titleColor = titleColor; this.contentColor = contentColor; modified = true;
    }
    public void remove() { deleted = true; modified = true; }
    public void publish() {
        published = visible && !deleted;
        publishedTitle = title; publishedContent = content; publishedTicker = ticker;
        publishedPlatform = platform; publishedSortOrder = sortOrder; publishedUrl = url;
        publishedTitleColor = titleColor; publishedContentColor = contentColor;
        modified = false;
    }
}
