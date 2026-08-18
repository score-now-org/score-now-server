package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.LeagueSeasonStandingsAppResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.StandingsDataResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
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

        LeagueSeason currentLeagueSeason = leagueSeasons.get(0);

        // 리그 시즌 정보를 기반으로 순위 관리 정보와 관리 타입을 조회
        LeagueSeasonStandings leagueSeasonStandings = leagueSeasonStandingsRepository.findByLeagueSeasonIdWithSeasonAndLeague(currentLeagueSeason.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "해당 리그에 대한 순위 관리 정보가 존재하지 않습니다."));

        LeagueSeason leagueSeason = leagueSeasonStandings.getLeagueSeason();
        League league = leagueSeason.getLeague();

        StandingsDataResponse resolvedStandingsDataResponse = resolveStandings(leagueSeasonStandings);

        return LeagueSeasonStandingsAppResponse.builder()
                .sportCode(league.getSport().getSportCode())
                .leagueId(leagueId)
                .leagueSeasonId(leagueSeason.getId())
                .leagueImageUrl(league.getImageUrl())
                .leagueName(league.resolveLeagueName())
                .seasonName(leagueSeason.getSeasonName())
                .startAt(leagueSeason.getSeasonStartAt())
                .endAt(leagueSeason.getSeasonEndAt())
                .standingsImageUrl(leagueSeasonStandings.getStandingsType() == LeagueSeasonStandingsType.IMAGE
                        ? leagueSeasonStandings.getImageUrl()
                        : null)
                .standings(resolvedStandingsDataResponse)
                .build();
    }

    private StandingsDataResponse resolveStandings(LeagueSeasonStandings leagueSeasonStandings) {

        if (leagueSeasonStandings.getStandingsType() == LeagueSeasonStandingsType.IMAGE) {
            return null;
        }

        LeagueSeason currentLeagueSeason = leagueSeasonStandings.getLeagueSeason();

        LeagueSeasonStandingsDataDocument document = leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(currentLeagueSeason.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "해당 리그에 대한 현재 시즌의 순위 정보가 존재하지 않습니다."));

        return standingsResolverRegistry.resolve(document);
    }

}
