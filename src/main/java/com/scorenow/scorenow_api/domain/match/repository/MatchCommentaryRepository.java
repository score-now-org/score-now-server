package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;

import java.util.List;

public interface MatchCommentaryRepository {
    MatchCommentaryDocument save(MatchCommentaryDocument commentary);

    List<MatchCommentaryDocument> findVisibleCommentaries(Long matchId);
}
