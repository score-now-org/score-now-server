package com.scorenow.scorenow_api.domain.league.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;
    private final SportRepository sportRepository;

    /**
     * 리그 등록 시 옵션 조회 (종목)
     */
    public AdminLeagueSearchOptionsResponse getSearchOptions() {
        return AdminLeagueSearchOptionsResponse.of(sportRepository.findAll());
    }

    /**
     * 리그 검색 (리그 ID, 리그명)
     */
    public List<AdminLeagueResponse> searchLeagues(LeagueSearchCondition condition) {
        LeagueSearchCondition safeCondition = condition != null ? condition : new LeagueSearchCondition();

        List<League> leagues = leagueRepository.searchLeagues(
                        safeCondition.getLeagueId(),
                        normalize(safeCondition.getKeyword())
                );

        return toAdminLeagueResponses(leagues);
    }

    /**
     * 리그 상세 조회 (리그 ID)
     */
    public AdminLeagueResponse searchLeagues(Long leagueId) {
        return leagueRepository.findByIdWithSport(leagueId)
                .map(league -> AdminLeagueResponse.from(league, findSyncEnabled(league)))
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));
    }

    /**
     * 리그 등록
     */
    @Transactional
    public AdminLeagueResponse createLeague(AdminLeagueCreateRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        League league = League.builder()
                .sportId(sport.getId())
                .eName(request.getEName())
                .kName(request.getKName())
                .sName(request.getSName())
                .teamDisplayOrder(resolveTeamDisplayOrder(request.getTeamDisplayOrder()))
                .dataOrigin(resolveLeagueDataOrigin(request))
                .build();

        League savedLeague = leagueRepository.save(league);

        Boolean syncEnabled = null;
        if (TRUE.equals(request.getExternalLinked())) {
            syncEnabled = TRUE.equals(request.getSyncEnabled());
            leagueExternalMappingRepository.save(LeagueExternalMapping.of(
                    request.getDataOrigin(),
                    normalizeApiLeagueId(request.getApiLeagueId()),
                    savedLeague.getId(),
                    syncEnabled
            ));
        }

        return AdminLeagueResponse.from(savedLeague, sport, syncEnabled);
    }

    /**
     * 리그 정보 수정
     */
    @Transactional
    public void updateLeague(Long leagueId, AdminLeagueUpdateRequest request) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        applyUpdates(league, request);

        log.info("리그 정보 수정 완료 - leagueId: {}", leagueId);
    }

    /**
     * 외부 API 리그 ID 수정
     * 참고) 현재는 외부 API 리그 ID 의 유효성 검증은 이루어지지 않고 있으며 오입력 하는 경우를 막을 수 없다.
     *      추후 개선이 필요한 부분이라고 생각
     */
    @Transactional
    public void updateApiLeagueId(Long leagueId, LeagueApiLeagueIdUpdateRequest request) {
        // 1. 매핑 정보 조회
        LeagueExternalMapping mapping = getLeagueExternalMapping(leagueId);

        String beforeApiLeagueId = mapping.getApiLeagueId();
        String afterApiLeagueId = normalizeApiLeagueId(request.getApiLeagueId());

        // 2. 변화가 없는 경우
        if (beforeApiLeagueId.equals(afterApiLeagueId)) {
            log.info("외부 API 리그 ID 변경 없음 - leagueId: {}, apiLeagueId: {}", leagueId, afterApiLeagueId);
            return;
        }

        mapping.updateApiLeagueId(afterApiLeagueId);

        log.info("외부 API 리그 ID 수정 완료 - leagueId: {}, before: {}, after: {}",
                leagueId, beforeApiLeagueId, afterApiLeagueId);
    }

    /**
     * 외부 API 경기 동기화 활성화 여부 수정
     */
    @Transactional
    public void updateSyncEnabled(Long leagueId, LeagueSyncEnabledUpdateRequest request) {
        LeagueExternalMapping mapping = getLeagueExternalMapping(leagueId);

        mapping.updateSyncEnabled(request.getSyncEnabled());

        log.info("외부 API 동기화 여부 수정 완료 - leagueId: {}, syncEnabled: {}",
                leagueId, request.getSyncEnabled());
    }

    /**
     * 리그 삭제
     */
    @Transactional
    public void deleteLeague(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }
        leagueRepository.deleteById(leagueId);
    }

    private void applyUpdates(League league, AdminLeagueUpdateRequest request) {
        if (request == null) {
            log.info("❌ 리그 업데이트 정보가 없습니다. leagueId={}, leagueName={}", league.getId(), league.getKName());
            return;
        }

        if (request.getSportId() != null) {
            if (!sportRepository.existsById(request.getSportId())) {
                throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
            }
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

    private DataOrigin resolveLeagueDataOrigin(AdminLeagueCreateRequest request) {
        // 외부 API 를 연동하지 않는 리그인 경우
        if (!TRUE.equals(request.getExternalLinked())) {
            return DataOrigin.MANUAL;
        }

        if (request.getDataOrigin() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 연동 리그는 dataOrigin 이 필수입니다.");
        }

        if (request.getDataOrigin() == DataOrigin.MANUAL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 연동 리그의 dataOrigin은 MANUAL일 수 없습니다.");
        }

        if (request.getApiLeagueId() == null || request.getApiLeagueId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 연동 리그는 apiLeagueId가 필수입니다.");
        }

        return request.getDataOrigin();
    }

    private LeagueExternalMapping getLeagueExternalMapping(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        if (league.getDataOrigin() == null || league.getDataOrigin() == DataOrigin.MANUAL) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API와 연동된 리그가 아닙니다.");
        }

        return leagueExternalMappingRepository
                .findByDataOriginAndInternalLeagueId(league.getDataOrigin(), league.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "외부 API 리그 매핑 정보를 찾을 수 없습니다."));
    }

    private String normalizeApiLeagueId(String apiLeagueId) {
        if (apiLeagueId == null || apiLeagueId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 리그 ID는 필수입니다.");
        }

        return apiLeagueId.trim();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private TeamDisplayOrder resolveTeamDisplayOrder(TeamDisplayOrder teamDisplayOrder) {
        return teamDisplayOrder != null ? teamDisplayOrder : TeamDisplayOrder.HOME_AWAY;
    }

    private List<AdminLeagueResponse> toAdminLeagueResponses(List<League> leagues) {
        Map<Long, Boolean> syncEnabledByLeagueId = findSyncEnabledByLeagueId(leagues);

        return leagues.stream()
                .map(league -> AdminLeagueResponse.from(league, syncEnabledByLeagueId.get(league.getId())))
                .toList();
    }

    private Map<Long, Boolean> findSyncEnabledByLeagueId(List<League> leagues) {
        List<Long> externalLeagueIds = leagues.stream()
                .filter(this::isExternalLeague)
                .map(League::getId)
                .toList();

        if (externalLeagueIds.isEmpty()) {
            return Map.of();
        }

        return leagueExternalMappingRepository.findByInternalLeagueIdIn(externalLeagueIds)
                .stream()
                .collect(Collectors.toMap(
                        LeagueExternalMapping::getInternalLeagueId,
                        LeagueExternalMapping::getSyncEnabled,
                        (first, ignored) -> first));
    }

    private Boolean findSyncEnabled(League league) {
        if (!isExternalLeague(league)) {
            return null;
        }

        return leagueExternalMappingRepository
                .findByDataOriginAndInternalLeagueId(league.getDataOrigin(), league.getId())
                .map(LeagueExternalMapping::getSyncEnabled)
                .orElse(null);
    }

    private boolean isExternalLeague(League league) {
        return league.getDataOrigin() != null && league.getDataOrigin() != DataOrigin.MANUAL;
    }

}
