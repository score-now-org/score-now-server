package com.scorenow.scorenow_api.domain.notice;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

public record NewsTickerRequest(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 10000) String content,
                                @NotNull Boolean visible,
                                @NotNull Notice.Platform platform,
                                @Size(max = 2048) @URL @Pattern(regexp = "(?i)https?://[^\\s]+") String url,
                                @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String titleColor,
                                @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String contentColor) {
    NoticeRequest asNotice() { return new NoticeRequest(title, content, true, visible, platform, null, url); }
}
