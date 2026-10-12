package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer.InplayMatchDetailSynchronizerRegistry;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.service.SportCodeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.IN_PLAY;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDetailSyncService {

    private final MatchRepository matchRepository;

    private final SportCodeResolver sportCodeResolver;

    private final InplayMatchDetailSynchronizerRegistry synchronizerRegistry;

    public void syncInplayMatchDetails() {
        // 진행중인 경기들을 조회한다. (단, 수동 경기의 경우 제외)
        List<Match> inplayMatches = matchRepository.findByStatusCodeAndIsManualFalse(IN_PLAY);

        if (inplayMatches.isEmpty()) {
            log.info("현재 INPLAY 상태의 경기가 없습니다.");
            return;
        }

        // "종목 별 Inplay 경기" 형태로 변환한다.
        Map<SportCode, List<Match>> matchesBySportCode = groupingBySportCode(inplayMatches);

        // 각 종목 별로 API 호출 및 동기화 작업을 진행한다.
        for (var entry : matchesBySportCode.entrySet()) {
            synchronizerRegistry.sync(entry.getKey(), entry.getValue());
        }
    }

    /**
     * "종목별 진행중 경기" 형태로 변환한다.
     * 참고: 기존에는 List<Match> 를 Map<SportCode, List<Match>> 형식으로 변경했는데, 해당 방식은 변환하는 과정 속에서
     * 순회할 때 마다, internal_sport_id 가 매핑된 sport_code 를 N 번 조회하게 된다. (불필요하게 너무 많이 조회하게 된다.)
     * List<Match> → Map<InternalSportId, List<Match>> → Map<SportCode, List<Match>> 로 전환하면
     * 최대 SportCode 수 만큼만 조회하면 되기 때문에 조회 횟수를 줄일 수 있다.
     */
    private Map<SportCode, List<Match>> groupingBySportCode(List<Match> inplayMatches) {
        Map<Long, List<Match>> matchesBySportId = inplayMatches.stream()
                .collect(Collectors.groupingBy(Match::getSportId));

        Map<SportCode, List<Match>> matchesBySportCode = new EnumMap<>(SportCode.class);
        for (var entry : matchesBySportId.entrySet()) {
            Long internalSportId = entry.getKey();
            List<Match> matches = entry.getValue();

            SportCode sportCode = sportCodeResolver.resolveByInternalSportId(internalSportId);

            matchesBySportCode
                    .computeIfAbsent(sportCode, newSportCode -> new ArrayList<>())
                    .addAll(matches);
        }

        return matchesBySportCode;
    }
}
