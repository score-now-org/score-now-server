package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;

    @Transactional(readOnly = true)
    public League getLeague(Long leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));
    }
}
