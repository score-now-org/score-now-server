package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;

import java.util.List;
import java.util.Optional;

public interface MatchDetailRepository {
    Optional<MatchDetailDocument> findById(Long matchId);

    List<MatchDetailDocument> findAllById(List<Long> matchIds);

    MatchDetailDocument save(MatchDetailDocument detail);

    void updateCurrentCommentary(Long matchId, String content, String commentaryId, boolean highlighted);

    void upsertMatchDetail(MatchDetailDocument matchDetailDocument);

    void createInitialMatchDetailIfAbsent(Long matchId, SportDetailType type);
}
