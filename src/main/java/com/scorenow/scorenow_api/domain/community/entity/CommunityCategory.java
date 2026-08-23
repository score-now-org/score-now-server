package com.scorenow.scorenow_api.domain.community.entity;

import java.util.Arrays;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityCategory {
	BASEBALL("야구", 2),
	SOCCER("축구", 3),
	BASKETBALL("농구", 4);

	private final String displayName;
	private final int displayOrder;

	public static CommunityCategory fromBoardType(CommunityBoardType boardType) {
		return Arrays.stream(values())
			.filter(category -> category.name().equals(boardType.name()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_INVALID_CATEGORY));
	}
}
