package com.scorenow.scorenow_api.domain.admin.member.dto;

import jakarta.validation.constraints.Size;

public record MemberEditRequest(@Size(max = 100) String name, @Size(max = 3) String countryCode) { }
