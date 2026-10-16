package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer.InitialSportDetailProviderRegistry;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchDetailInitializationService {

    private final MatchDetailRepository matchDetailRepository;
    private final InitialSportDetailProviderRegistry providerRegistry;

    public void createIfAbsent(Long matchId, SportCode sportCode) {
        SportDetail initialSportDetail = providerRegistry.create(sportCode);

        MatchDetailDocument initialDocument = MatchDetailDocument.builder()
                .id(matchId)
                .type(sportCode)
                .sportDetail(initialSportDetail)
                .build();

        matchDetailRepository.createInitialMatchDetailIfAbsent(initialDocument);
    }
}
