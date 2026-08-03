package com.scorenow.scorenow_api.domain.community.dto.request;

import com.scorenow.scorenow_api.domain.community.entity.CommunityReactionType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommunityReactionRequest {

	@NotNull(message = "추천 타입은 필수입니다.")
	private CommunityReactionType type;
}
