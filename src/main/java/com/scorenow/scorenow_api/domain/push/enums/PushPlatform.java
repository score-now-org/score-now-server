package com.scorenow.scorenow_api.domain.push.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PushPlatform {
	AOS("aos"),
	IOS("ios");

	private final String topic;
}
