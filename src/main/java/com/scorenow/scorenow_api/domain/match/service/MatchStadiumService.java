package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchStadiumMappingResponse;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MatchStadiumService {
    private final MatchRepository matchRepository;
    private final StadiumRepository stadiumRepository;
    private final SportRepository sportRepository;

    @Transactional(readOnly = true)
    public MatchStadiumMappingResponse getMatchStadiumMapping(Long matchId) {
        Match match = findMatch(matchId);

        // 임시 경기장이 할당되어 있는 경우
        if (match.getTemporaryStadium() != null) {
            return MatchStadiumMappingResponse.temporary(match.getTemporaryStadium());
        }

        // 경기장 정보가 아예 없는 경우
        if (match.getStadiumId() == null) {
            return MatchStadiumMappingResponse.none();
        }

        // 경기장 정보가 있는 경우
        Stadium stadium = stadiumRepository.findById(match.getStadiumId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        Sport sport = sportRepository.findById(stadium.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        return MatchStadiumMappingResponse.registered(stadium, sport);
    }

    /**
     * 임시 경기장 정보 할당 (Match 에 직접 할당하며, 일회성이므로 Stadium Table 에서 조회 불가능)
     */
    @Transactional
    public void assignTemporaryStadiumToMatch(Long matchId, String stadiumName, String city) {
        Match match = findMatch(matchId);

        match.assignTemporaryStadium(
                requireText(stadiumName, "임시 경기장명"),
                normalize(city)
        );
    }

    /**
     * 사전에 등록되어 있는 경기장으로 할당
     */
    @Transactional
    public void assignStadiumToMatch(Long matchId, Long stadiumId) {
        Match match = findMatch(matchId);

        Stadium stadium = stadiumRepository
                .findActiveById(stadiumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        match.updateStadiumId(stadium.getId());
    }

    @Transactional
    public void removeTemporaryStadiumFromMatch(Long matchId) {
        Match match = findMatch(matchId);

        if (match.getTemporaryStadium() == null) {
            throw new BusinessException(ErrorCode.MATCH_STADIUM_MAPPING_INVALID_STATE);
        }

        match.clearTemporaryStadium();
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, fieldName + "은(는) 비워둘 수 없습니다.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
