package com.scorenow.scorenow_api.domain.community.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityBoardType {
	ALL("전체", 0),
	POPULAR("인기", 1),
	BASEBALL("야구", 2),
	SOCCER("축구", 3),
	BASKETBALL("농구", 4);

	private final String displayName;
	private final int displayOrder;

	public boolean isCategory() {
		return this != ALL && this != POPULAR;
	}

	public boolean isPopular() {
		return this == POPULAR;
	}
}
