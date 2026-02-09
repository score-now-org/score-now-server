package com.scorenow.scorenow_api.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchType {
	A("A", "자동"),
	M("M", "수동");

	private final String code;
	private final String description;

	public static MatchType fromCode(String code){
		if(code == null) return A;
		for(MatchType type: values()){
			if(type.code.equalsIgnoreCase(code)){
				return type;
			}
		}
		return A;
	}
}
