package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;

import java.util.HashMap;
import java.util.List;
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
        List<MatchDetailDocument> matchDetailDocuments = database.values().stream()
                .filter(matchDetail -> matchId.equals(matchDetail.getId()))
                .toList();

        matchDetailDocuments.forEach(matchDetailDocument -> matchDetailDocument.updateCurrentCommentary(content, commentaryId));

        return matchDetailDocuments.size();
    }

}
