package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics.MatchStatisticsResolverRegistry;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchStatisticsAppQueryService {

    private final MatchDetailRepository matchDetailRepository;
    private final MatchStatisticsResolverRegistry statisticsResolverRegistry;

    public MatchStatisticsResponse getStatistics(Long matchId) {

        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));

        return statisticsResolverRegistry.resolve(matchDetailDocument);
    }
}
