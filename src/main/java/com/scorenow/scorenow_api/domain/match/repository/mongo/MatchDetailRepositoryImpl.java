package com.scorenow.scorenow_api.domain.match.repository.mongo;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MatchDetailRepositoryImpl implements MatchDetailRepository {
    private final MatchDetailMongoRepository mongoRepository;

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        return mongoRepository.save(matchDetail);
    }

    @Override
    public Optional<MatchDetailDocument> findById(String matchId) {
        return mongoRepository.findById(matchId);
    }

    @Override
    public void updateCurrentCommentary(String matchId, String content, String commentaryId) {
        mongoRepository.updateCurrentCommentary(matchId, content, commentaryId);
    }
}
