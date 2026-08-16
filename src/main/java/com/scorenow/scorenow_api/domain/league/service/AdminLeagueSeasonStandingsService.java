package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.GroupMapping;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsGroupMappingsUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsTypeUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsGroupMappingsResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.LeagueSeasonStandingsSyncService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueSeasonStandingsService {

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;
    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;
    private final MongoTemplate mongoTemplate;

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
     * 리그 시즌 순위 관리 방식 검색 (전체 조회, 리그명, 리그ID)
     */
    public Page<AdminLeagueSeasonStandingsResponse> searchLeagueSeasonStandings(
            LeagueSeasonStandingsSearchCondition condition,
            Pageable pageable) {

        LeagueSeasonStandingsSearchCondition safeCondition = condition != null
                ? condition
                : new LeagueSeasonStandingsSearchCondition();

        // 리그 시즌 순위 관리 조회
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

    /**
     * 리그 시즌 순위 그룹 표시 설정 조회
     */
    public AdminLeagueSeasonStandingsGroupMappingsResponse getGroupMappings(Long leagueSeasonId) {

        LeagueSeasonStandingsDataDocument document = getStandingDataDocument(leagueSeasonId);

        if (!(document.getData() instanceof FootballStandingsData footballData)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 리그 순위 데이터 형식입니다.");
        }

        List<FootballStandingsData.StandingsGroup> dataGroups = footballData.getGroups();
        List<GroupMapping> groupMappings = document.getGroupMappings();

        if (dataGroups == null || dataGroups.isEmpty() || groupMappings == null || groupMappings.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 그룹 정보가 없습니다. 먼저 외부 동기화를 진행해주세요.");
        }

        // key:groupKey, value:GroupMapping 인 Map 생성
        Map<String, GroupMapping> mappingByGroupKey = toMappingByGroupKey(groupMappings);

        // 동일한 groupKey 로 구성되어있는지 검증
        validateSameGroupKeys(
                dataGroups.stream().map(FootballStandingsData.StandingsGroup::getGroupKey).toList(),
                groupMappings.stream().map(GroupMapping::getGroupKey).toList()
        );

        // 순위 그룹 정보 조립
        List<AdminLeagueSeasonStandingsGroupMappingsResponse.Group> groups = dataGroups.stream()
                .map(group -> {
                    GroupMapping mapping = mappingByGroupKey.get(group.getGroupKey());

                    return AdminLeagueSeasonStandingsGroupMappingsResponse.Group.builder()
                            .groupKey(group.getGroupKey())
                            .externalName(group.getExternalName())
                            .externalGroupName(group.getExternalGroupName())
                            .displayName(mapping.getDisplayName())
                            .displayOrder(mapping.getDisplayOrder())
                            .visibleInMatchList(mapping.isVisibleInMatchList())
                            .build();
                })
                .sorted(Comparator.comparing(
                        AdminLeagueSeasonStandingsGroupMappingsResponse.Group::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        return AdminLeagueSeasonStandingsGroupMappingsResponse.builder()
                .leagueSeasonId(document.getLeagueSeasonId())
                .groups(groups)
                .build();
    }

    /**
     * 리그 시즌 순위 그룹 표시 설정 수정
     * - groupKey는 외부 그룹 식별값이므로 변경하지 않고 표시명, 표시 순서, 경기 목록 노출 여부만 수정한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateGroupMappings(Long leagueSeasonId, LeagueSeasonStandingsGroupMappingsUpdateRequest request) {

        LeagueSeasonStandingsDataDocument document = getStandingDataDocument(leagueSeasonId);
        List<GroupMapping> currentMappings = document.getGroupMappings();

        if (currentMappings == null || currentMappings.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 그룹 정보가 없습니다. 먼저 외부 동기화를 진행해주세요.");
        }

        List<LeagueSeasonStandingsGroupMappingsUpdateRequest.Group> requestedGroups = request.getGroups();

        validateSameGroupKeys(
                currentMappings.stream().map(GroupMapping::getGroupKey).toList(),
                requestedGroups.stream().map(LeagueSeasonStandingsGroupMappingsUpdateRequest.Group::getGroupKey).toList()
        );

        // 요청으로 들어온 displayOrder 가 0부터 연속된 값인지 확인 (예: 0,1,2...)
        validateDisplayOrders(requestedGroups);

        // GroupMapping 업데이트 정보 조립
        List<GroupMapping> updatedMappings = requestedGroups.stream()
                .sorted(Comparator.comparing(LeagueSeasonStandingsGroupMappingsUpdateRequest.Group::getDisplayOrder))
                .map(group -> GroupMapping.builder()
                        .groupKey(group.getGroupKey())
                        .displayName(group.getDisplayName().trim())
                        .displayOrder(group.getDisplayOrder())
                        .visibleInMatchList(group.isVisibleInMatchList())
                        .build())
                .toList();

        // GroupMapping 정보 업데이트
        Query query = Query.query(Criteria.where("leagueSeasonId").is(leagueSeasonId));
        Update update = new Update().set("groupMappings", updatedMappings);

        mongoTemplate.updateFirst(query, update, LeagueSeasonStandingsDataDocument.class);
    }

    private LeagueSeasonStandingsDataDocument getStandingDataDocument(Long leagueSeasonId) {
        return leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "리그 시즌 순위 데이터를 찾을 수 없습니다. 먼저 외부 동기화를 진행해주세요."
                ));
    }

    private Map<String, GroupMapping> toMappingByGroupKey(List<GroupMapping> mappings) {
        try {
            return mappings.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            GroupMapping::getGroupKey,
                            Function.identity()
                    ));
        } catch (IllegalStateException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "저장된 리그 순위 그룹 key가 올바르지 않습니다.");
        }
    }

    private void validateSameGroupKeys(List<String> expectedKeys, List<String> requestedKeys) {
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        Set<String> requestedKeySet = new HashSet<>(requestedKeys);

        boolean hasDuplicatedKey = expectedKeySet.size() != expectedKeys.size()
                || requestedKeySet.size() != requestedKeys.size();

        if (hasDuplicatedKey || !expectedKeySet.equals(requestedKeySet)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "순위 그룹 구성이 일치하지 않습니다. expected=" + expectedKeySet + ", requested=" + requestedKeySet
            );
        }
    }

    private void validateDisplayOrders(List<LeagueSeasonStandingsGroupMappingsUpdateRequest.Group> groups) {
        Set<Integer> requestedOrders = groups.stream()
                .map(LeagueSeasonStandingsGroupMappingsUpdateRequest.Group::getDisplayOrder)
                .collect(Collectors.toSet());

        Set<Integer> expectedOrders = IntStream.range(0, groups.size())
                .boxed()
                .collect(Collectors.toSet());

        if (!requestedOrders.equals(expectedOrders)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "순위 그룹 표시 순서는 0부터 중복 없이 연속되어야 합니다."
            );
        }
    }


    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
