package com.scorenow.scorenow_api.domain.match.service.appresponse;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppSportLiveSummaryResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MatchAppSportResponseMapperRegistry {

    private final Map<SportCode, MatchAppSportResponseMapper> mappers;

    public MatchAppSportResponseMapperRegistry(List<MatchAppSportResponseMapper> mapperList) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MatchAppSportResponseMapper::type,
                        mapper -> mapper
                ));
    }

    public MatchAppSportLiveSummaryResponse mapLiveSummary(
            Match match,
            MatchDetailDocument matchDetailDocument) {

        MatchAppSportResponseMapper mapper = findMapper(matchDetailDocument);
        if (mapper == null) {
            return null;
        }

        return mapper.toLiveSummary(match, matchDetailDocument.getSportDetail());
    }

    public MatchAppSportDetailResponse mapDetail(MatchDetailDocument matchDetailDocument) {
        MatchAppSportResponseMapper mapper = findMapper(matchDetailDocument);
        if (mapper == null) {
            return null;
        }

        return mapper.toDetail(matchDetailDocument.getSportDetail());
    }

    private MatchAppSportResponseMapper findMapper(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null || matchDetailDocument.getType() == null) {
            return null;
        }

        return mappers.get(matchDetailDocument.getType());
    }
}
