package com.scorenow.scorenow_api.domain.team.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamCreateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public AdminTeamResponse createTeam(AdminTeamCreateRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        Team team = Team.builder()
                .sport(sport)
                .type(request.getTeamType())
                .kName(requireText(request.getKName(), "한글 팀명"))
                .eName(requireText(request.getEName(), "영문 팀명"))
                .sName(requireText(request.getSName(), "축약 팀명").toUpperCase(Locale.ROOT))
                .imageUrl(normalizeImageUrl(request.getImageUrl()))
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
                normalize(safeCondition.getKeyword()),
                pageable
        )
                .map(AdminTeamResponse::from);
    }

    /**
     * 팀 관리 화면 선택 옵션 조회
     */
    public AdminTeamSearchOptionsResponse getSearchOptions() {
        var teamTypes = Arrays.stream(TeamType.values())
                .map(teamType -> AdminTeamSearchOptionsResponse.TeamTypeOption.builder()
                        .code(teamType.name())
                        .name(teamType.getDisplayName())
                        .build())
                .toList();

        var sports = sportRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(sport -> AdminTeamSearchOptionsResponse.SportOption.builder()
                        .id(sport.getId())
                        .code(sport.getSportCode().name())
                        .name(sport.resolveSportName())
                        .build())
                .toList();

        return AdminTeamSearchOptionsResponse.builder()
                .teamTypes(teamTypes)
                .sports(sports)
                .build();
    }

    /**
     * 팀 정보 수정
     */
    @Transactional
    public void updateTeam(Long teamId, AdminTeamUpdateRequest request) {
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

        if (team.getDataOrigin() != MANUAL) {
            throw new BusinessException(ErrorCode.TEAM_DELETE_NOT_ALLOWED);
        }

        team.deactivate();

        log.info("팀 삭제 완료 - teamId: {}", teamId);
    }

    private void applyUpdates(Team team, AdminTeamUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수정할 팀 정보가 없습니다.");
        }
        if (!hasAnyUpdate(request)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수정할 값이 하나 이상 필요합니다.");
        }

        if (request.getTeamType() != null) {
            team.updateType(request.getTeamType());
        }
        if (request.getKName() != null) {
            team.updateKName(requireText(request.getKName(), "한글 팀명"));
        }
        if (request.getEName() != null) {
            team.updateEName(requireText(request.getEName(), "영문 팀명"));
        }
        if (request.getSName() != null) {
            team.updateSName(requireText(request.getSName(), "축약 팀명").toUpperCase(Locale.ROOT));
        }
        if (request.getImageUrl() != null) {
            team.updateImageUrl(requireImageUrl(request.getImageUrl()));
        }
        if (request.getCc() != null) {
            String cc = normalize(request.getCc());
            team.updateCountryCode(cc != null ? cc.toUpperCase(Locale.ROOT) : null);
        }
    }

    private boolean hasAnyUpdate(AdminTeamUpdateRequest request) {
        return request.getTeamType() != null
                || request.getKName() != null
                || request.getEName() != null
                || request.getSName() != null
                || request.getImageUrl() != null
                || request.getCc() != null;
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, fieldName + "은(는) 비워둘 수 없습니다.");
        }
        return normalized;
    }

    private String requireImageUrl(String value) {
        String normalized = requireText(value, "팀 이미지 URL");
        return validateImageUrl(normalized);
    }

    private String normalizeImageUrl(String value) {
        String normalized = normalize(value);
        return normalized != null ? validateImageUrl(normalized) : null;
    }

    private String validateImageUrl(String normalized) {
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "팀 이미지 URL 형식이 올바르지 않습니다.");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
