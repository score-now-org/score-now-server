package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;

import java.util.List;
import java.util.Optional;

public interface MatchDetailRepository {
    Optional<MatchDetailDocument> findById(Long matchId);

    List<MatchDetailDocument> findAllById(List<Long> matchIds);

    MatchDetailDocument save(MatchDetailDocument detail);

    long updateCurrentCommentary(Long matchId, String content, String commentaryId);

    void upsertCurrentCommentary(Long matchId, String content, String commentaryId);

    void upsertMatchDetail(MatchDetailDocument matchDetailDocument);
}
