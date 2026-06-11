package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.mapper.MatchMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMatchService {

    private final MatchRepository matchRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final SportRepository sportRepository;

    private final MatchMapper matchMapper;

    private final MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;

    /**
     * 경기 리스트 조회
     */
    public Page<MatchListResponse> getMatches(MatchSearchCondition condition, Pageable pageable) {
        return matchRepository.searchMatches(condition, pageable)
                .map(matchMapper::toResponse);
    }

    /**
     * 경기 수동 등록
     * <p>
     * 경기 수동 등록할 때, 리그 정보도 넣어준다.
     * 기본적으로 리그 정보를 자동으로 따라가게 하고, 그게 아니면 home-away 로 세팅
     * <p>
     * 정리하면 MatchCreateRequest 에서는 displayOrder 를 따로 전달받지 않고 League 정보 기반으로 추론한다.
     * League 로 추론이 불가능한 경우, 기본값인 HOME_AWAY 들어감.
     */
    @Transactional
    public MatchListResponse createMatch(MatchCreateRequest request) {
        validateMatchCreateRequest(request);

        Match match = matchMapper.toEntity(request);

        League league = leagueRepository.findById(request.getLeagueId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        TeamDisplayOrder teamDisplayOrder = matchTeamDisplayOrderPolicy.decide(league);
        match.updateTeamDisplayOrder(teamDisplayOrder);

        matchRepository.save(match);

        log.info("수동 경기 등록 완료 - matchId: {}", match.getId());

        return matchRepository.findByIdWithRelations(match.getId())
                .map(matchMapper::toResponse)
                .orElseGet(() -> matchMapper.toResponse(match));
    }

    /**
     * 경기 수정
     */
    @Transactional
    public void updateMatch(Long matchId, MatchUpdateRequest request) {
        Match match = findMatchById(matchId);

        applyUpdates(match, request);

        log.info("경기 수정 완료 - matchId: {}", matchId);
    }

    // === Validation Methods ===

    private void validateMatchCreateRequest(MatchCreateRequest request) {
        if (!leagueRepository.existsById(request.getLeagueId())) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }
        if (!teamRepository.existsByIdAndIsActiveTrue(request.getHomeId())) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "홈팀을 찾을 수 없습니다.");
        }
        if (!teamRepository.existsByIdAndIsActiveTrue(request.getAwayId())) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "원정팀을 찾을 수 없습니다.");
        }
        if (request.getSportId() != null && !sportRepository.existsById(request.getSportId())) {
            throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
        }
    }

    // === Helper Methods ===

    private Match findMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private void applyUpdates(Match match, MatchUpdateRequest request) {
        if (request.getStartAt() != null) {
            match.updateStartAt(request.getStartAt());
        }

        if (request.getStatusCode() != null) {
            try {
                match.updateStatus(MatchStatus.valueOf(request.getStatusCode()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
            }
        }

        if (request.getHomeScore() != null) {
            match.updateHomeScore(request.getHomeScore());
        }

        if (request.getAwayScore() != null) {
            match.updateAwayScore(request.getAwayScore());
        }

        if (request.getIsActive() != null) {
            match.updateIsActive(request.getIsActive());
        }

        if (request.getTeamDisplayOrder() != null) {
            match.updateTeamDisplayOrder(request.getTeamDisplayOrder());
        }
    }


}
