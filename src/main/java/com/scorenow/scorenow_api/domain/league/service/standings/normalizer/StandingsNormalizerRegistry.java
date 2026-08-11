package com.scorenow.scorenow_api.domain.league.service.standings.normalizer;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StandingsNormalizerRegistry {

    private final Map<Long, StandingsNormalizer> normalizers;

    public StandingsNormalizerRegistry(List<StandingsNormalizer> normalizerList) {
        this.normalizers = normalizerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        StandingsNormalizer::sportId,
                        normalizer -> normalizer));
    }

    public LeagueSeasonStandingsDataDocument normalize(
            BetsStandingsResponse.Result result,
            LeagueSeasonStandingsSyncTarget syncTarget) {

        StandingsNormalizer standingsNormalizer = normalizers.get(syncTarget.getSportId());
        return standingsNormalizer.normalize(result, syncTarget);
    }
}
