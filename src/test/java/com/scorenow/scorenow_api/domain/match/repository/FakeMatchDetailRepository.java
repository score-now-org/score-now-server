package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeMatchDetailRepository implements MatchDetailRepository {

    private final Map<String, MatchDetailDocument> database = new HashMap<>();

    @Override
    public Optional<MatchDetailDocument> findById(String matchId) {
        return Optional.ofNullable(database.get(matchId));
    }

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        String id = matchDetail.getId();
        database.put(id, matchDetail);
        return database.get(id);
    }

    @Override
    public long updateCurrentCommentary(String matchId, String content, String commentaryId) {
        MatchDetailDocument matchDetailDocument = database.get(matchId);
        matchDetailDocument.updateCurrentCommentary(content, commentaryId);

        return 1;
    }

}
