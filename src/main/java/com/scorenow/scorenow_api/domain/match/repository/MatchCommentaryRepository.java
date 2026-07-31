package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;

import java.util.List;
import java.util.Optional;

public interface MatchCommentaryRepository {
    MatchCommentaryDocument save(MatchCommentaryDocument commentary);

    Optional<MatchCommentaryDocument> findById(String id);

    List<MatchCommentaryDocument> findVisibleCommentaries(Long matchId);
}
