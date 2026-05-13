package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeMatchDetailRepository implements MatchDetailRepository {

    private final AtomicLong idGenerator = new AtomicLong(0);
    private final Map<Long, MatchDetailDocument> database = new HashMap<>();

    @Override
    public Optional<MatchDetailDocument> findById(Long matchId) {
        return Optional.ofNullable(database.get(matchId));
    }

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        Long id = idGenerator.getAndIncrement();
        database.put(id, matchDetail);
        ReflectionTestUtils.setField(matchDetail, "id", id);
        return database.get(id);
    }

    @Override
    public long updateCurrentCommentary(Long matchId, String content, String commentaryId) {
        List<MatchDetailDocument> matchDetailDocuments = database.values().stream()
                .filter(matchDetail -> matchId.equals(matchDetail.getId()))
                .toList();

        matchDetailDocuments.forEach(matchDetailDocument -> matchDetailDocument.updateCurrentCommentary(content, commentaryId));

        return matchDetailDocuments.size();
    }

}
