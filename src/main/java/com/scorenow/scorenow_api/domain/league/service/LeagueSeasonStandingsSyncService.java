package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.league.mapper.LeagueSeasonStandingsMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueSeasonStandingsSyncService {

    private final LeagueSeasonStandingsSyncTargetReader syncTargetReader;
    private final BetsApiClient betsApiClient;
    private final MongoTemplate mongoTemplate;

    private final LeagueSeasonStandingsMapper mapper;

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

        LeagueSeasonStandingsDataDocument document = mapper.toDocument(result, syncTarget);

        Query query = new Query(Criteria.where("leagueSeasonId").is(document.getLeagueSeasonId()));

        Update update = new Update();
        update.set("leagueId", document.getLeagueId());
        update.set("apiLeagueId", document.getApiLeagueId());
        update.set("dataOrigin", document.getDataOrigin());
        update.set("leagueSeasonId", document.getLeagueSeasonId());
        update.set("externalSeasonName", document.getExternalSeasonName());
        update.set("externalSeasonStartAt", document.getExternalSeasonStartAt());
        update.set("externalSeasonEndAt", document.getExternalSeasonEndAt());
        update.set("standingsTable", document.getStandingsTable());

        mongoTemplate.upsert(query, update, LeagueSeasonStandingsDataDocument.class);
    }
}
