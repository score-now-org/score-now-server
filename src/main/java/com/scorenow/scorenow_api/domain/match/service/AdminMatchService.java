package com.scorenow.scorenow_api.domain.match.service;

import java.util.List;
import java.util.Objects;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.service.stage.MatchStageResponseResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.AdminMatchSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchTeamCandidateResponse;
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

    /* 수동 경기 등록 과정에서 팀명 조회 시 최대 조회 결과 수 */
    private static final int TEAM_CANDIDATE_LIMIT = 10;

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final SportRepository sportRepository;

    private final AdminFeaturedMatchService adminFeaturedMatchService;

    private final MatchMapper matchMapper;

    private final MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;

    private final MatchRealtimeEventPublisher eventPublisher;

    private final MatchStageResponseResolver matchStageResponseResolver;

    /**
     * 경기 리스트 조회
     */
    public Page<MatchListResponse> getMatches(MatchSearchCondition condition, Pageable pageable) {
        return matchRepository.searchMatches(condition, pageable)
                .map(matchMapper::toResponse);
    }

    /**
     * 경기 리스트 검색 옵션 조회
     */
    public AdminMatchSearchOptionsResponse getSearchOptions() {
        return AdminMatchSearchOptionsResponse.of(
                sportRepository.findAll(),
                leagueRepository.findAll());
    }

    /**
     * 경기 등록용 팀 후보 검색
     */
    public List<MatchTeamCandidateResponse> getTeamCandidates(String keyword, Long sportId) {

        return teamRepository.searchMatchTeamCandidates(
                        sportId,
                        normalize(keyword),
                        PageRequest.of(0, TEAM_CANDIDATE_LIMIT)
                )
                .stream()
                .map(MatchTeamCandidateResponse::from)
                .toList();
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
    public void createMatch(MatchCreateRequest request) {
        validateMatchCreateRequest(request);

        Match match = matchMapper.toEntity(request);
        match.updateDataOrigin(DataOrigin.MANUAL);

        League league = leagueRepository.findById(request.getLeagueId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        TeamDisplayOrder teamDisplayOrder = matchTeamDisplayOrderPolicy.decide(league);
        match.updateTeamDisplayOrder(teamDisplayOrder);

        Match savedMatch = matchRepository.save(match);

        SportDetailType type = SportDetailType.fromSportId(savedMatch.getSportId());
        matchDetailRepository.createInitialMatchDetailIfAbsent(savedMatch.getId(), type);

        log.info("수동 경기 등록 완료 - matchId: {}", match.getId());
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
        // 1. 리그가 존재하지 않는 경우
        if (!leagueRepository.existsById(request.getLeagueId())) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }

        // 2. 존재하지 않는 팀인 경우
        if (!teamRepository.existsByIdAndIsActiveTrue(request.getHomeId())) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "홈팀을 찾을 수 없습니다.");
        }

        if (!teamRepository.existsByIdAndIsActiveTrue(request.getAwayId())) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "원정팀을 찾을 수 없습니다.");
        }

        // 3. 존재하지 않는 종목인 경우
        if (request.getSportId() == null || !sportRepository.existsById(request.getSportId())) {
            throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
        }

        // 4. 홈팀과 어웨이팀이 동일한 경우
        if (request.getHomeId().equals(request.getAwayId())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "홈팀과 어웨이팀이 같습니다.");
        }
    }

    // === Helper Methods ===

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Match findMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private void applyUpdates(Match match, MatchUpdateRequest request) {
        if (request.getStartAt() != null) {

            // 일자 기준 변경이 발생했을 때, 상단고정/핫매치 설정 해제 처리
            if (match.isStartDateChanged(request.getStartAt())) {
                adminFeaturedMatchService.deleteFeaturedMatchByMatchId(match.getId());
            }

            match.updateStartAt(request.getStartAt());
        }

        /* TODO: 점수 수정 없어질 수 있음. 참고*/
        if (request.getHomeScore() != null) {
            match.updateHomeScore(request.getHomeScore());
        }

        if (request.getAwayScore() != null) {
            match.updateAwayScore(request.getAwayScore());
        }

        if (request.getStatusCode() != null) {
            updateMatchStatusAndPublishEvent(match, request.getStatusCode());
        }

        if (request.getIsManual() != null) {
            // 수동 관리 경기의 경우에는 자동 관리 경기로 전환이 불가능하다.
            if (match.getDataOrigin() == DataOrigin.MANUAL && !request.getIsManual()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수동 등록 경기는 자동 관리 경기로 전환할 수 없습니다.");
            }
            match.updateIsManual(request.getIsManual());
        }

        if (request.getIsActive() != null) {
            match.updateIsActive(request.getIsActive());
        }

        if (request.getTeamDisplayOrder() != null) {
            match.updateTeamDisplayOrder(request.getTeamDisplayOrder());
        }
    }

    private void updateMatchStatusAndPublishEvent(Match match, MatchStatus newStatus) {

        if (Objects.equals(match.getStatusCode(), newStatus)) {
            return;
        }

        match.updateStatus(newStatus);

        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(match.getId())
                .orElse(null);

        MatchStageResponse matchStageResponse = matchStageResponseResolver.resolve(match, matchDetailDocument);
        eventPublisher.publishMatchStatusChanged(match.getId(), newStatus, matchStageResponse.getDisplayText());
    }


}
