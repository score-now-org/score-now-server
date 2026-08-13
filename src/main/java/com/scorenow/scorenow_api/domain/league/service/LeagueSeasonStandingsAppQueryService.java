package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.LeagueSeasonStandingsAppResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.StandingsDataResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.resolver.StandingsResolverRegistry;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueSeasonStandingsAppQueryService {

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    private final StandingsResolverRegistry standingsResolverRegistry;

    public LeagueSeasonStandingsAppResponse getLeagueSeasonStandings(Long leagueId) {

        // 리그의 현재 시즌 정보를 조회 및 검증
        List<LeagueSeason> leagueSeasons = leagueSeasonRepository.findCurrentSeasonByLeagueId(leagueId);

        if (leagueSeasons == null || leagueSeasons.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "해당 리그에 대한 현재 시즌 정보가 존재하지 않습니다.");
        }

        if (leagueSeasons.size() > 1) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "해당 리그에 대한 현재 시즌 정보가 2개 이상 입니다.");
        }

        // 리그 시즌 정보를 기반으로 해당 시즌의 순위 데이터 조회
        LeagueSeason leagueSeason = leagueSeasons.get(0);
        LeagueSeasonStandingsDataDocument document = leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(leagueSeason.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "해당 리그에 대한 현재 시즌의 순위 정보가 존재하지 않습니다."));

        // 응답 조립을 위한 부가적인 정보 조회 (리그, 리그 시즌 순위 관리, 순위 데이터)
        League league = leagueSeason.getLeague();

        LeagueSeasonStandings leagueSeasonStandings = leagueSeasonStandingsRepository.findByLeagueSeasonId(leagueSeason.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "해당 리그에 대한 순위 관리 정보가 존재하지 않습니다."));

        StandingsDataResponse resolvedStandingsDataResponse = standingsResolverRegistry.resolve(document);

        return LeagueSeasonStandingsAppResponse.builder()
                .leagueId(leagueId)
                .leagueSeasonId(leagueSeason.getId())
                .leagueImageUrl(league.getImageUrl())
                .leagueName(league.resolveLeagueName())
                .seasonName(leagueSeason.getSeasonName())
                .startAt(leagueSeason.getSeasonStartAt())
                .endAt(leagueSeason.getSeasonEndAt())
                .standingsImageUrl(leagueSeasonStandings.getImageUrl())
                .standings(resolvedStandingsDataResponse)
                .build();
    }

}
