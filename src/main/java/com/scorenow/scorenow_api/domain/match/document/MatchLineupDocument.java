package com.scorenow.scorenow_api.domain.match.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.global.entity.BaseDocument;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "match_lineups")
public class MatchLineupDocument extends BaseDocument {

	@Id
	private Long id;

	private Long sportId;
	private String apiMatchId;

	private LineupSide home;
	private LineupSide away;

	public static MatchLineupDocument create(Long matchId, Long sportId, String apiMatchId) {
		MatchLineupDocument doc = new MatchLineupDocument();
		doc.id = matchId;
		doc.sportId = sportId;
		doc.apiMatchId = apiMatchId;
		return doc;
	}
}