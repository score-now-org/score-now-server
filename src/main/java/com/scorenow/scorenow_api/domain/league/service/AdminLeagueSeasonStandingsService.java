package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsTypeUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueSeasonStandingsService {

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;
    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    private final LeagueSeasonStandingsSyncService leagueSeasonStandingsSyncService;

    private final FileStorage fileStorage;

    /**
     * 리그 순위 관리 방식 등록
     * - League 의 DataOrigin 이 MANUAL 이면, LeagueStandingType 은 IMAGE 만 허용
     * - LeagueStandingType 이 EXTERNAL_DATA 이면, LeagueExternalMapping 이 있어야 함
     */
    @Transactional
    public void createLeagueSeasonStandings(LeagueSeasonStandingsCreateRequest createRequest) {
        Long leagueSeasonId = createRequest.getLeagueSeasonId();
        LeagueSeasonStandingsType standingsType = createRequest.getStandingsType();

        LeagueSeason leagueSeason = leagueSeasonRepository.findByIdAndIsActiveTrue(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAMETER, "존재하지 않는 시즌입니다."));

        // 이미 관리되는 LeagueSeason 인지 확인
        if (leagueSeasonStandingsRepository.existsByLeagueSeasonId(leagueSeasonId)) {
            throw new BusinessException(ErrorCode.LEAGUE_ALREADY_EXISTS, "이미 순위 관리 중인 시즌입니다.");
        }

        LeagueSeasonStandings.validateStandingsTypeAllowed(leagueSeason, standingsType);

        // 순위 정보를 외부 데이터에 기반하는 경우, 사전에 리그 외부 매핑 정보가 있는지 확인
        if (standingsType == LeagueSeasonStandingsType.EXTERNAL_DATA) {
            League league = leagueSeason.getLeague();

            leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(league.getDataOrigin(), league.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 리그 매핑이 필요합니다."));
        }

        // 등록
        leagueSeasonStandingsRepository.save(LeagueSeasonStandings.from(leagueSeason, standingsType));
    }

    /**
     * 리그 시즌 순위 검색 (전체 조회, 리그명, 리그ID)
     */
    public Page<AdminLeagueSeasonStandingsResponse> searchLeagueSeasonStandings(
            LeagueSeasonStandingsSearchCondition condition,
            Pageable pageable) {

        LeagueSeasonStandingsSearchCondition safeCondition = condition != null
                ? condition
                : new LeagueSeasonStandingsSearchCondition();

        // 리그 시즌 순위 조회
        Page<LeagueSeasonStandings> seasonStandings = leagueSeasonStandingsRepository.searchLeagueSeasonStandings(
                safeCondition.getLeagueId(),
                normalize(safeCondition.getLeagueName()),
                pageable
        );

        return seasonStandings.map(AdminLeagueSeasonStandingsResponse::from);
    }

    /**
     * 리그 순위 관리 방식 타입 변경
     */
    @Transactional
    public void updateLeagueSeasonStandingsType(Long leagueSeasonId, LeagueSeasonStandingsTypeUpdateRequest updateRequest) {

        LeagueSeasonStandingsType newType = updateRequest.getStandingsType();

        LeagueSeasonStandings seasonStanding = leagueSeasonStandingsRepository
                .findByLeagueSeasonIdWithSeasonAndLeague(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리그 시즌 순위 정보를 찾을 수 없습니다."));

        // 변화가 없는 경우 종료
        if (seasonStanding.getStandingsType() == newType) {
            return;
        }

        LeagueSeason leagueSeason = seasonStanding.getLeagueSeason();
        LeagueSeasonStandings.validateStandingsTypeAllowed(leagueSeason, newType);
        League league = leagueSeason.getLeague();

        // 외부 데이터를 사용하게끔 변경한다면, League External Mapping 에 사전 등록되어 있어야 한다.
        if (newType == LeagueSeasonStandingsType.EXTERNAL_DATA) {
            leagueExternalMappingRepository.findByDataOriginAndInternalLeagueId(league.getDataOrigin(), league.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 API 리그 매핑이 필요합니다."));
        }

        seasonStanding.updateStandingsType(leagueSeason, newType);
    }

    /**
     * 리그 순위 관리 방식 삭제
     */
    @Transactional
    public void deleteLeagueSeasonStandings(Long leagueSeasonId) {

        // LeagueSeasonStanding 조회
        LeagueSeasonStandings seasonStanding = leagueSeasonStandingsRepository
                .findByLeagueSeasonId(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리그 시즌 순위 정보를 찾을 수 없습니다."));

        leagueSeasonStandingsMongoRepository.deleteByLeagueSeasonId(leagueSeasonId);
        leagueSeasonStandingsRepository.delete(seasonStanding);
    }

    /**
     * 리그 시즌 순위 이미지 등록 (관리 타입이 IMAGE 인 경우에만 가능)
     */
    @Transactional
    public void uploadLeagueSeasonStandingsImage(
            Long leagueSeasonId,
            MultipartFile image) {

        // LeagueSeasonStanding 조회
        LeagueSeasonStandings seasonStanding = leagueSeasonStandingsRepository
                .findByLeagueSeasonId(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리그 시즌 순위 정보를 찾을 수 없습니다."));

        // 리그 순위가 이미지 타입으로 관리되지 않는 경우 업로드에 실패
        if (seasonStanding.getStandingsType() != LeagueSeasonStandingsType.IMAGE) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미지 타입으로 관리 중인 리그만 이미지를 등록할 수 있습니다.");
        }

        // 리그 순위 이미지 업로드
        String imageUrl = fileStorage.uploadFile(image);

        // 리그 순위 이미지 등록 또는 갱신
        seasonStanding.updateImageUrl(imageUrl);
    }

    /**
     * 리그 시즌 순위 데이터 동기화 (관리 타입이 DATA 인 경우에만 가능)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncLeagueSeasonStandingsData(Long leagueSeasonId) {
        LeagueSeasonStandings seasonStanding = leagueSeasonStandingsRepository
                .findByLeagueSeasonId(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리그 시즌 순위 정보를 찾을 수 없습니다."));

        if (seasonStanding.getStandingsType() != LeagueSeasonStandingsType.EXTERNAL_DATA) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "외부 데이터 타입으로 관리 중인 리그만 동기화할 수 있습니다.");
        }

        leagueSeasonStandingsSyncService.sync(leagueSeasonId);
    }


    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
