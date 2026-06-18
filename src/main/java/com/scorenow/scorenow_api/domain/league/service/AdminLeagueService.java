package com.scorenow.scorenow_api.domain.league.service;

import java.util.List;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueService {

    private final LeagueRepository leagueRepository;
    private final SportRepository sportRepository;

    public List<AdminLeagueResponse> searchLeagues(String keyword) {
        return leagueRepository.searchByKeyword(keyword)
                .stream()
                .map(AdminLeagueResponse::from)
                .toList();
    }

    public AdminLeagueResponse searchLeagues(Long leagueId) {
        return leagueRepository.findById(leagueId)
                .map(AdminLeagueResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));
    }

    @Transactional
    public AdminLeagueResponse createLeague(LeagueCreateRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        League league = League.builder()
                .sportId(sport.getId())
                .eName(request.getEName())
                .kName(request.getKName())
                .sName(request.getSName())
                .teamDisplayOrder(request.getTeamDisplayOrder())
                .dataOrigin(DataOrigin.MANUAL)
                .build();

        return AdminLeagueResponse.from(leagueRepository.save(league));
    }

    @Transactional
    public void updateLeague(Long leagueId, LeagueUpdateRequest request) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        applyUpdates(league, request);

        log.info("리그 정보 수정 완료 - leagueId: {}", leagueId);
    }

    @Transactional
    public void deleteLeague(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }
        leagueRepository.deleteById(leagueId);
    }

    private void applyUpdates(League league, LeagueUpdateRequest request) {
        if (request == null) {
            log.info("❌ 리그 업데이트 정보가 없습니다. leagueId={}, leagueName={}", league.getId(), league.getKName());
            return;
        }

        if (request.getSportId() != null) {
            league.updateSportId(request.getSportId());
        }
        if (request.getEName() != null) {
            league.updateEName(request.getEName());
        }
        if (request.getKName() != null) {
            league.updateKName(request.getKName());
        }
        if (request.getSName() != null) {
            league.updateSName(request.getSName());
        }
        if (request.getTeamDisplayOrder() != null) {
            league.updateTeamDisplayOrder(request.getTeamDisplayOrder());
        }
    }
}
