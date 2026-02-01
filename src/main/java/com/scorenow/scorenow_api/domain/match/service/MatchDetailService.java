package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDetailService {
    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final BetsApiClient betsApiClient;

    @Transactional
    public void updateInplayMatchDetail(com.scorenow.scorenow_api.domain.match.entity.Match match) {
        String pureId = match.getId().replaceAll("[^0-9]", "");
        BetsViewResponse response = betsApiClient.getEventView(pureId);

        if (response == null || !response.hasResult()) {
            log.warn("❌ 실패: ID {}에 대한 API 응답 데이터가 없습니다.", pureId);
            return;
        }

        BetsViewResponse.ViewResult apiResult = response.getResults().get(0);

        match.setHomeScore(apiResult.getHomeScore());
        match.setAwayScore(apiResult.getAwayScore());

        MatchDetailDocument detail = response.toDocument(match.getId());
        matchDetailRepository.save(detail);

        log.info("✅ 성공: {} 경기 상세 데이터(MySQL & MongoDB) 동기화 완료", match.getId());
    }


    private void updateMySqlScore(String eventId, BetsViewResponse.ViewResult result) {
        matchRepository.findById(eventId).ifPresent(match -> {
            match.setHomeScore(result.getHomeScore());
            match.setAwayScore(result.getAwayScore());
        });
    }
}