package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeMatchCommentaryRepository implements MatchCommentaryRepository {

    private final Map<String, MatchCommentaryDocument> database = new HashMap<>();

    @Override
    public MatchCommentaryDocument save(MatchCommentaryDocument commentary) {
        String id = generateId();
        database.put(id, commentary);

        ReflectionTestUtils.setField(commentary, "id", id);
        return database.get(id);
    }

    @Override
    public List<MatchCommentaryDocument> findVisibleCommentaries(Long matchId) {
        return database.values().stream()
                .filter(matchCommentary -> matchId.equals(matchCommentary.getMatchId()))
                .filter(MatchCommentaryDocument::isVisible)
                .toList();
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }
}
