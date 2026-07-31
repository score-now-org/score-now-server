package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchCommentaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MatchCommentaryRepositoryImpl implements MatchCommentaryRepository {
    private final MatchCommentaryMongoRepository mongoRepository;

    @Override
    public MatchCommentaryDocument save(MatchCommentaryDocument commentary) {
        return mongoRepository.save(commentary);
    }

    @Override
    public Optional<MatchCommentaryDocument> findById(String id) {
        return mongoRepository.findById(id);
    }


    @Override
    public List<MatchCommentaryDocument> findVisibleCommentaries(Long matchId) {
        return mongoRepository.findVisibleCommentaries(matchId);
    }
}
