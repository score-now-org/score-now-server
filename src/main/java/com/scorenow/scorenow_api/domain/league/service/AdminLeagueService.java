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
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueService {

    private static final String LEAGUE_EMBLEM_DIRECTORY = "league-emblems";

    private final LeagueRepository leagueRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;
    private final SportRepository sportRepository;
    private final FileStorage fileStorage;

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
        return createLeague(request, null);
    }

    @Transactional
    public AdminLeagueResponse createLeague(AdminLeagueCreateRequest request, MultipartFile image) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        DataOrigin dataOrigin = resolveLeagueDataOrigin(request);
        Boolean syncEnabled = TRUE.equals(request.getExternalLinked())
                ? TRUE.equals(request.getSyncEnabled())
                : null;
        String apiLeagueId = TRUE.equals(request.getExternalLinked())
                ? normalizeApiLeagueId(request.getApiLeagueId())
                : null;
        UploadedEmblem uploadedEmblem = uploadEmblem(image);

        League league = League.builder()
                .sportId(sport.getId())
                .eName(request.getEName())
                .kName(request.getKName())
                .sName(request.getSName())
                .imageUrl(uploadedEmblem == null ? null : uploadedEmblem.url())
                .teamDisplayOrder(resolveTeamDisplayOrder(request.getTeamDisplayOrder()))
                .dataOrigin(dataOrigin)
                .build();

        League savedLeague = leagueRepository.save(league);

        if (TRUE.equals(request.getExternalLinked())) {
            leagueExternalMappingRepository.save(LeagueExternalMapping.of(
                    request.getDataOrigin(),
                    apiLeagueId,
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
        updateLeague(leagueId, request, null);
    }

    @Transactional
    public void updateLeague(Long leagueId, AdminLeagueUpdateRequest request, MultipartFile image) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

        validateEmblemUpdate(request, image);
        applyUpdates(league, request);
        updateEmblem(league, request, image);

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

    private void validateEmblemUpdate(AdminLeagueUpdateRequest request, MultipartFile image) {
        if (request != null && TRUE.equals(request.getRemoveEmblem()) && hasImage(image)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "엠블럼 제거와 새 이미지 등록을 동시에 요청할 수 없습니다."
            );
        }
    }

    /**
     * 엠블럼 수정 (삭제 포함)
     */
    private void updateEmblem(League league, AdminLeagueUpdateRequest request, MultipartFile image) {
        String previousImageUrl = league.getImageUrl();

        // 엠블럼 삭제
        if (request != null && TRUE.equals(request.getRemoveEmblem())) {
            league.removeImageUrl();
            registerPreviousEmblemDeletionAfterCommit(previousImageUrl);
            return;
        }

        if (!hasImage(image)) {
            return;
        }

        UploadedEmblem uploadedEmblem = uploadEmblem(image);
        league.updateImageUrl(uploadedEmblem.url());
        registerPreviousEmblemDeletionAfterCommit(previousImageUrl);
    }

    /**
     * 엠블럼 파일 서버에 업로드
     */
    private UploadedEmblem uploadEmblem(MultipartFile image) {
        if (!hasImage(image)) {
            return null;
        }

        String key = fileStorage.uploadFileAndReturnKey(image, LEAGUE_EMBLEM_DIRECTORY);
        registerNewEmblemDeletionOnRollback(key);
        return new UploadedEmblem(key, fileStorage.getFileUrl(key));
    }

    /**
     * DB 트랜잭션 롤백 시 새로 업로드한 엠블럼을 삭제하도록 등록
     */
    private void registerNewEmblemDeletionOnRollback(String newEmblemKey) {
        // 현재 스레드에 콜백을 등록할 수 있는 트랜잭션 동기화가 활성화되어 있는지 확인
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    safelyDeleteFile(newEmblemKey);
                    return;
                }

                if (status == TransactionSynchronization.STATUS_UNKNOWN) {
                    log.error("트랜잭션 결과를 확인할 수 없어 신규 리그 엠블럼을 삭제하지 않았습니다. key={}", newEmblemKey);
                }
            }
        });
    }

    /**
     * DB 트랜잭션 커밋 후 기존 엠블럼을 삭제하도록 등록
     */
    private void registerPreviousEmblemDeletionAfterCommit(String previousImageUrl) {
        if (!fileStorage.isManagedFileUrl(previousImageUrl)) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safelyDeleteFileByUrl(previousImageUrl);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safelyDeleteFileByUrl(previousImageUrl);
            }
        });
    }

    private void safelyDeleteFile(String key) {
        try {
            fileStorage.deleteFile(key);
        } catch (RuntimeException exception) {
            log.error("GCS 리그 엠블럼 보상 삭제 실패. key={}", key, exception);
        }
    }

    private void safelyDeleteFileByUrl(String imageUrl) {
        try {
            fileStorage.deleteFileByUrl(imageUrl);
        } catch (RuntimeException exception) {
            log.error("GCS 기존 리그 엠블럼 정리 실패. imageUrl={}", imageUrl, exception);
        }
    }

    private boolean hasImage(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    private record UploadedEmblem(String key, String url) {
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
