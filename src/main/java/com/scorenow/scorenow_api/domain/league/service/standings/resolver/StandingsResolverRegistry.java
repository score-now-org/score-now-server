package com.scorenow.scorenow_api.domain.league.service.standings.resolver;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.StandingsDataResponse;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StandingsResolverRegistry {

    private final Map<SportCode, StandingsResolver> resolvers;

    public StandingsResolverRegistry(List<StandingsResolver> resolverList) {

        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        StandingsResolver::sportCode,
                        Function.identity()
                ));

    }

    public StandingsDataResponse resolve(LeagueSeasonStandingsDataDocument document) {

        SportCode sportCode = document.getSportCode();

        StandingsResolver standingsResolver = resolvers.get(sportCode);

        if (standingsResolver == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. sportCode=" + sportCode);
        }

        return standingsResolver.resolve(document);
    }
}
