package com.scorenow.scorenow_api.domain.community.dto.response;

import java.util.Arrays;
import java.util.List;

import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityBoardResponse {

	private CommunityBoardType boardType;
	private String displayName;
	private int displayOrder;
	private boolean postable;

	public static List<CommunityBoardResponse> defaults() {
		return Arrays.stream(CommunityBoardType.values())
			.sorted((left, right) -> Integer.compare(left.getDisplayOrder(), right.getDisplayOrder()))
			.map(CommunityBoardResponse::of)
			.toList();
	}

	private static CommunityBoardResponse of(CommunityBoardType boardType) {
		return CommunityBoardResponse.builder()
			.boardType(boardType)
			.displayName(boardType.getDisplayName())
			.displayOrder(boardType.getDisplayOrder())
			.postable(boardType.isCategory())
			.build();
	}
}
