package com.scorenow.scorenow_api.domain.league.service.standings;

import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.league.entity.*;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueSeasonStandingsSyncTargetReader {

    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;

    public LeagueSeasonStandingsSyncTarget read(Long leagueSeasonId) {
        // LeagueSeasonStanding 조회
        LeagueSeasonStandings seasonStanding = leagueSeasonStandingsRepository
                .findByLeagueSeasonIdWithSeasonAndLeague(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리그 시즌 순위 정보를 찾을 수 없습니다."));

        // 리그 순위가 EXTERNAL_DATA 타입으로 관리되지 않는 경우 업로드에 실패
        if (seasonStanding.getStandingsType() != LeagueSeasonStandingsType.EXTERNAL_DATA) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "데이터 타입으로 관리 중인 리그만 동기화 할 수 있습니다.");
        }

        LeagueSeason leagueSeason = seasonStanding.getLeagueSeason();
        League league = leagueSeason.getLeague();
        Sport sport = league.getSport();

        // 외부 API 리그 ID 조회
        LeagueExternalMapping leagueExternalMapping = leagueExternalMappingRepository
                .findByDataOriginAndInternalLeagueId(league.getDataOrigin(), league.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND, "외부 API 리그 ID 가 존재하지 않습니다."));

        return LeagueSeasonStandingsSyncTarget.builder()
                .sportId(league.getSportId())
                .sportCode(sport.getSportCode())
                .leagueId(league.getId())
                .leagueSeasonId(leagueSeasonId)
                .leagueSeasonStandingsId(seasonStanding.getId())
                .apiLeagueId(leagueExternalMapping.getApiLeagueId())
                .dataOrigin(leagueExternalMapping.getDataOrigin())
                .build();
    }
}
