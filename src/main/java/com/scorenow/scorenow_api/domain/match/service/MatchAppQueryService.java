package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchAppQueryService {

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;

    public List<MatchAppResponse> getMatches(LocalDate date, Long sportId, Long leagueId) {
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of(SEOUL_TIME_ZONE));
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        // 1. 경기 목록을 조회한다.
        List<Match> matches = matchRepository.findAppMatches(
                startAt,
                endAt,
                sportId,
                leagueId,
                List.of(MatchStatus.IN_PLAY, MatchStatus.NOT_STARTED, MatchStatus.ENDED),
                MatchStatus.IN_PLAY,
                MatchStatus.NOT_STARTED,
                MatchStatus.ENDED
        );

        if (matches.isEmpty()) {
            log.info("해당 날짜 {} 에 대하여 조회되는 경기가 없습니다. sportId={}, leagueId={}", date, sportId, leagueId);
            return List.of();
        }

        // 2. 경기 목록 조회 결과 기반으로 경기 상세 정보를 조회한다.
        List<Long> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        Map<Long, MatchDetailDocument> detailMap = matchDetailRepository.findAllById(matchIds).stream()
                .collect(toMap(
                        MatchDetailDocument::getId,
                        Function.identity(),
                        (first, second) -> first));

        // 3. (1)과 (2)에서 얻어 온 결과값을 활용하여 응답을 만든 후 반환한다.
        return matches.stream()
                .map(match -> MatchAppResponse.from(match, detailMap.get(match.getId())))
                .toList();
    }

}
