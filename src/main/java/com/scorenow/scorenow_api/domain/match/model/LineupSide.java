package com.scorenow.scorenow_api.domain.match.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LineupSide {

	private String teamId;
	private String teamEname;

	private String formation;
	private String uniformColor;

	@Builder.Default
	private List<LineupPlayer> startingLineup = new ArrayList<>();

	@Builder.Default
	private List<LineupPlayer> substitutes = new ArrayList<>();
}
