package com.scorenow.scorenow_api.domain.admin.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberActionRequest(@NotBlank String action, @Size(max = 500) String reason) { }
