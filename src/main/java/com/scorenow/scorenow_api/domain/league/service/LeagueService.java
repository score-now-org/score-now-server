package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;

    /**
     * 경기장 생성
     */
    @Transactional
    public League getOrCreateLeague(
            final DataOrigin dataOrigin,
            final Long internalSportId,
            final String apiLeagueId,
            final String name,
            final String cc) {

        // 1. 전달받은 외부 정보를 기반으로 League External Mapping 테이블에 데이터가 있는지 확인
        Optional<LeagueExternalMapping> leagueMappingInfo = leagueExternalMappingRepository.findByExternalInfo(dataOrigin, apiLeagueId);

        // 2. 매핑 정보에서 internal league id 를 추출하고, 이를 기반으로 League 엔티티 반환
        if (leagueMappingInfo.isPresent()) {
            Long internalLeagueId = leagueMappingInfo.get().getInternalLeagueId();
            return leagueRepository.findById(internalLeagueId)
                    .orElseGet(() -> createAndMapLeague(dataOrigin, internalSportId, apiLeagueId, name, cc));
        }

        return createAndMapLeague(dataOrigin, internalSportId, apiLeagueId, name, cc);
    }

    private League createAndMapLeague(
            DataOrigin dataOrigin,
            Long internalSportId,
            String apiLeagueId,
            String name,
            String cc) {

        League savedLeague = leagueRepository.save(League.builder()
                .sportId(internalSportId)
                .kName(name)
                .eName(name)
                .cc(cc)
                .dataOrigin(dataOrigin)
                .build());

        leagueExternalMappingRepository.save(LeagueExternalMapping.of(dataOrigin, apiLeagueId, savedLeague.getId(), true));

        return savedLeague;
    }
}
