package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;

import java.util.Optional;

public interface MatchDetailRepository {
    Optional<MatchDetailDocument> findById(String eventId);

    MatchDetailDocument save(MatchDetailDocument detail);

    void updateCurrentCommentary(String matchId, String content, String commentaryId);
}
