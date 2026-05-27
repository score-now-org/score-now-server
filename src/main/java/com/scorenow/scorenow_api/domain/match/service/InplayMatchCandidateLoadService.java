package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.redis.InplayMatchRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.*;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayMatchCandidateLoadService {

    private static final ZoneId SEOUL_TIME_ZONE_ID = ZoneId.of(SEOUL_TIME_ZONE);

    private final MatchRepository matchRepository;
    private final InplayMatchRedisRepository inplayMatchRedisRepository;

    public void loadInplayCandidatesToCache(LocalDateTime standardTime) {
        ZonedDateTime start = standardTime.atZone(SEOUL_TIME_ZONE_ID);
        ZonedDateTime end = start.plusHours(INPLAY_CANDIDATES_SCAN_INTERVAL_HOURS);

        List<MatchCandidate> candidates = matchRepository.findMatchesStartingWithin(NOT_STARTED, start.toLocalDateTime(), end.toLocalDateTime());

        log.info("{}시간 이내 시작 예정 경기 건수 : {}", INPLAY_CANDIDATES_SCAN_INTERVAL_HOURS, candidates.size());

        for (MatchCandidate candidate : candidates) {
            ZonedDateTime startAt = candidate.getStartAt().atZone(SEOUL_TIME_ZONE_ID);
            inplayMatchRedisRepository.addCandidate(candidate, startAt);
        }
    }
}
