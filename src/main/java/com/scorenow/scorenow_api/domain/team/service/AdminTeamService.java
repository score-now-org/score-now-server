package com.scorenow.scorenow_api.domain.team.service;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamCreateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminTeamService {

    private final TeamRepository teamRepository;
    private final SportRepository sportRepository;

    /**
     * 팀 수동 등록
     */
    @Transactional
    public AdminTeamResponse createTeam(TeamCreateRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        Team team = Team.builder()
                .sport(sport)
                .type(request.getTeamType())
                .kName(request.getKName())
                .eName(request.getEName())
                .sName(request.getSName())
                .imageUrl(request.getImageUrl())
                .cc(request.getCc())
                .dataOrigin(MANUAL)
                .build();

        return AdminTeamResponse.from(teamRepository.save(team));
    }

    /**
     * 팀 검색
     */
    public Page<AdminTeamResponse> searchTeams(TeamSearchCondition condition, Pageable pageable) {
        TeamSearchCondition safeCondition = condition != null ? condition : new TeamSearchCondition();

        return teamRepository.searchTeams(
                        safeCondition.getTeamId(),
                        normalize(safeCondition.getKName()),
                        normalize(safeCondition.getEName()),
                        pageable
                )
                .map(AdminTeamResponse::from);
    }

    /**
     * 팀 정보 수정 기능
     */
    @Transactional
    public void updateTeam(Long teamId, TeamUpdateRequest request) {
        Team team = teamRepository.findByIdAndIsActiveTrue(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        applyUpdates(team, request);

        log.info("팀 정보 수정 완료 - teamId: {}", teamId);
    }

    /**
     * 팀 삭제 기능
     */
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findByIdAndIsActiveTrue(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        team.deactivate();

        log.info("팀 삭제 완료 - teamId: {}", teamId);
    }

    private void applyUpdates(Team team, TeamUpdateRequest request) {
        if (request == null) {
            log.info("❌ 팀 업데이트 정보가 없습니다. teamId={}, teamName={}", team.getId(), team.getKName());
            return;
        }

        if (request.getTeamType() != null) {
            team.updateType(request.getTeamType());
        }
        if (request.getKName() != null) {
            team.updateKName(request.getKName());
        }
        if (request.getEName() != null) {
            team.updateEName(request.getEName());
        }
        if (request.getSName() != null) {
            team.updateSName(request.getSName());
        }
        if (request.getImageUrl() != null) {
            team.updateImageUrl(request.getImageUrl());
        }
        if (request.getCc() != null) {
            team.updateCountryCode(request.getCc());
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
