package com.scorenow.scorenow_api.domain.notice;

import jakarta.validation.constraints.*;
public record NoticeRequest(@NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content, @NotNull Boolean ticker, @NotNull Boolean visible,
        @NotNull Notice.Platform platform, @Min(1) Integer sortOrder, @Size(max = 2048) String url) {}
