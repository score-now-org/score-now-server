package com.scorenow.scorenow_api.domain.league.service.standings.normalizer;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StandingsNormalizerRegistry {

    private final Map<SportCode, StandingsNormalizer> normalizers;

    public StandingsNormalizerRegistry(List<StandingsNormalizer> normalizerList) {
        this.normalizers = normalizerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        StandingsNormalizer::sportCode,
                        normalizer -> normalizer));
    }

    public LeagueSeasonStandingsDataDocument normalize(
            BetsStandingsResponse.Result result,
            LeagueSeasonStandingsSyncTarget syncTarget) {

        StandingsNormalizer standingsNormalizer = normalizers.get(syncTarget.getSportCode());

        if (standingsNormalizer == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "순위 동기화를 지원하지 않는 종목입니다. sportCode=" + syncTarget.getSportCode());
        }

        return standingsNormalizer.normalize(result, syncTarget);
    }
}
