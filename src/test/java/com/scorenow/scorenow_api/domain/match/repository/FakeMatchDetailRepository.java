package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FakeMatchDetailRepository implements MatchDetailRepository {

    private final AtomicLong idGenerator = new AtomicLong(0);
    private final Map<Long, MatchDetailDocument> database = new HashMap<>();

    @Override
    public Optional<MatchDetailDocument> findById(Long matchId) {
        return Optional.ofNullable(database.get(matchId));
    }

    @Override
    public List<MatchDetailDocument> findAllById(List<Long> matchIds) {
        return matchIds.stream()
                .map(database::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        Long id = matchDetail.getId() != null ? matchDetail.getId() : idGenerator.getAndIncrement();
        database.put(id, matchDetail);
        ReflectionTestUtils.setField(matchDetail, "id", id);
        return database.get(id);
    }

    @Override
    public void upsertCurrentCommentary(Long matchId, String content, String commentaryId, boolean highlighted) {
        MatchDetailDocument saved = database.get(matchId);

        if (saved == null) {
            MatchDetailDocument matchDetailDocument = MatchDetailDocument.builder()
                    .id(matchId)
                    .currentCommentaryId(commentaryId)
                    .currentCommentary(content)
                    .currentCommentaryHighlighted(highlighted)
                    .build();

            database.put(matchId, matchDetailDocument);
            return;
        }

        saved.updateCurrentCommentary(content, commentaryId, highlighted);
    }

    @Override
    public void upsertMatchDetail(MatchDetailDocument matchDetailDocument) {
        Long id = matchDetailDocument.getId();
        MatchDetailDocument saved = database.get(id);

        if (saved == null) {
            database.put(id, matchDetailDocument);
            return;
        }

        ReflectionTestUtils.setField(saved, "type", matchDetailDocument.getType());
        ReflectionTestUtils.setField(saved, "homeScore", matchDetailDocument.getHomeScore());
        ReflectionTestUtils.setField(saved, "awayScore", matchDetailDocument.getAwayScore());
        ReflectionTestUtils.setField(saved, "sportDetail", matchDetailDocument.getSportDetail());
    }

}
