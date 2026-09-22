package com.scorenow.scorenow_api.domain.community.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommunityCommentReactionRequest {

	@NotNull(message = "댓글 추천 여부는 필수입니다.")
	private Boolean liked;
}
