package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchStadiumService {
    private final MatchRepository matchRepository;
    private final StadiumRepository stadiumRepository;

    /**
     * 임시 경기장 정보 할당 (Match 에 직접 할당하며, 일회성이므로 Stadium Table 에서 조회 불가능)
     */
    @Transactional
    public void assignTemporaryStadiumToMatch(Long matchId, String stadiumName, String city) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        match.assignTemporaryStadium(stadiumName, city);
    }

    /**
     * 사전에 등록되어 있는 경기장으로 할당
     */
    @Transactional
    public void assignStadiumToMatch(Long matchId, Long stadiumId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        Stadium stadium = stadiumRepository
                .findById(stadiumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        match.updateStadiumId(stadium.getId());
    }

}
