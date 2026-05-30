package com.scorenow.scorenow_api.domain.league.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.LeagueAdminResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueAdminService {

    private final LeagueRepository leagueRepository;

    public List<LeagueAdminResponse> getLeagues(String keyword) {
        return leagueRepository.searchByKeyword(keyword)
                .stream()
                .map(LeagueAdminResponse::from)
                .toList();
    }

    @Transactional
    public LeagueAdminResponse createLeague(LeagueCreateRequest request) {

        League league = League.builder()
                .sportId(request.getSportId())
                .eName(request.getEName())
                .kName(request.getKName())
                .sName(request.getSName())
                .build();

        return LeagueAdminResponse.from(leagueRepository.save(league));
    }

    @Transactional
    public void updateLeague(Long leagueId, LeagueUpdateRequest request) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        if (request.getSportId() != null) league.updateSportId(request.getSportId());
        if (request.getEName() != null) league.updateEName(request.getEName());
        if (request.getKName() != null) league.updateKName(request.getKName());
        if (request.getSName() != null) league.updateSName(request.getSName());
        if (request.getTeamDisplayOrder() != null) league.updateTeamDisplayOrder(request.getTeamDisplayOrder());
    }

    @Transactional
    public void deleteLeague(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }
        leagueRepository.deleteById(leagueId);
    }
}
