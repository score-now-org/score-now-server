package com.scorenow.scorenow_api.domain.league.service.standings;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.normalizer.StandingsNormalizerRegistry;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueSeasonStandingsSyncService {

    private final LeagueSeasonStandingsSyncTargetReader syncTargetReader;
    private final BetsApiClient betsApiClient;
    private final MongoTemplate mongoTemplate;

    private final LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    private final StandingsNormalizerRegistry standingsNormalizerRegistry;

    /**
     * 리그 시즌 순위 동기화
     */
    public void sync(Long leagueSeasonId) {

        // 동기화 대상 관련 정보 조회
        LeagueSeasonStandingsSyncTarget syncTarget = syncTargetReader.read(leagueSeasonId);

        // 리그 순위 API 호출
        BetsStandingsResponse response = betsApiClient.getStandings(syncTarget.getApiLeagueId());

        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 API 호출에 실패했습니다.");
        }

        if (!response.hasResults()) {
            log.info("🟠리그 순위 데이터가 없습니다.");
            return;
        }

        BetsStandingsResponse.Result result = response.firstResult()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAMETER, "리그 순위 API 응답에 유효한 결과가 없습니다."));

        // 외부 API 응답값을 기반으로 새로 만든 리그 순위 정보
        LeagueSeasonStandingsDataDocument newDocument = standingsNormalizerRegistry.normalize(result, syncTarget);

        // 기존에 저장되어 있던 리그 순위 정보
        Optional<LeagueSeasonStandingsDataDocument> currentDocument =
                leagueSeasonStandingsMongoRepository.findByLeagueSeasonId(leagueSeasonId);

        // 기존 리그 순위 정보가 없는 경우 -> 초기화 진행
        if (currentDocument.isEmpty()) {
            initializeDocument(newDocument);
            return;
        }

        // 기존 리그 순위 정보가 있는 경우 -> 비교 후 업데이트 진행
        compareAndUpdate(currentDocument.get(), newDocument);
    }

    /**
     * 기존 순위 데이터와 신규 순위 데이터 비교 후 업데이트 진행
     * - 단, 그룹 구성이 동일하지 않은 경우 업데이트 실패
     */
    private void compareAndUpdate(
            LeagueSeasonStandingsDataDocument currentDocument,
            LeagueSeasonStandingsDataDocument newDocument) {

        Set<String> currentGroupKeys = getGroupKeys(currentDocument.getGroupMappings());
        Set<String> newGroupKeys = getGroupKeys(newDocument.getGroupMappings());

        // 그룹 구성이 동일하지 않다면 업데이트 불가능
        if (!currentGroupKeys.equals(newGroupKeys)) {
            log.warn("🔴순위 그룹 구성이 일치하지 않습니다. leagueSeasonId={}, expected={}, observed={}",
                    currentDocument.getLeagueSeasonId(),
                    currentGroupKeys,
                    newGroupKeys);

            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "외부 순위 그룹 구성이 변경되었습니다. expected=" + currentGroupKeys + ", observed=" + newGroupKeys
            );
        }

        // 그룹 구성이 동일하다면 업데이트 진행 (groupMappings 는 제외)
        Query query = new Query(Criteria.where("leagueSeasonId").is(newDocument.getLeagueSeasonId()));

        Update update = commonDataUpdate(newDocument);

        mongoTemplate.updateFirst(query, update, LeagueSeasonStandingsDataDocument.class);

        log.info("🟢순위 동기화를 완료했습니다. leagueSeasonId:{}", newDocument.getLeagueSeasonId());
    }

    private Set<String> getGroupKeys(List<LeagueSeasonStandingsDataDocument.GroupMapping> groupMappings) {

        if (groupMappings == null || groupMappings.isEmpty()) {
            return Set.of();
        }

        return groupMappings.stream()
                .map(LeagueSeasonStandingsDataDocument.GroupMapping::getGroupKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 순위 데이터 초기화용
     */
    private void initializeDocument(LeagueSeasonStandingsDataDocument newDocument) {
        Query query = new Query(Criteria.where("leagueSeasonId").is(newDocument.getLeagueSeasonId()));

        Update update = commonDataUpdate(newDocument).set("groupMappings", newDocument.getGroupMappings());

        mongoTemplate.upsert(query, update, LeagueSeasonStandingsDataDocument.class);

        log.info("🟢순위 초기화를 완료했습니다. leagueSeasonId:{}", newDocument.getLeagueSeasonId());
    }

    /**
     * groupMappings 를 제외한 나머지 공통 필드 업데이트
     * - groupMappings 의 경우에는 맨 처음 순위 데이터가 초기화된 이후에는 수정되면 안되기 때문이다.
     */
    private Update commonDataUpdate(LeagueSeasonStandingsDataDocument newDocument) {
        Update update = new Update();
        update.set("sportId", newDocument.getSportId());
        update.set("sportCode", newDocument.getSportCode());
        update.set("leagueId", newDocument.getLeagueId());
        update.set("apiLeagueId", newDocument.getApiLeagueId());
        update.set("dataOrigin", newDocument.getDataOrigin());
        update.set("leagueSeasonId", newDocument.getLeagueSeasonId());
        update.set("leagueSeasonStandingsId", newDocument.getLeagueSeasonStandingsId());
        update.set("syncedAt", newDocument.getSyncedAt());
        update.set("data", newDocument.getData());
        update.set("externalSeason", newDocument.getExternalSeason());
        return update;
    }
}
