package com.scorenow.scorenow_api.domain.league.service.standings;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.GroupMapping;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsMongoRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.standings.extractor.TeamStandingExtractorRegistry;
import com.scorenow.scorenow_api.domain.league.service.standings.model.ExternalTeamStandingKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.LeagueTeamKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
@RequiredArgsConstructor
public class TeamStandingLookupService {

    private final TeamExternalMappingRepository teamExternalMappingRepository;

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueSeasonStandingsMongoRepository leagueSeasonStandingsMongoRepository;

    private final TeamStandingExtractorRegistry teamStandingExtractorRegistry;

    public Map<LeagueTeamKey, List<TeamStandingSummary>> getTeamStandings(Map<Long, Set<Long>> teamIdsByLeague, LocalDate date) {

        if (teamIdsByLeague.isEmpty()) {
            return Map.of();
        }

        // 각 경기의 리그 정보와 경기 시작일을 활용하여 LeagueSeason 을 조회한다.
        List<LeagueSeason> leagueSeasons = findLeagueSeasons(teamIdsByLeague.keySet(), date);

        // LeagueSeason 을 바탕으로 관리 타입이 EXTERNAL_DATA 인 LeagueSeasonStandings 를 조회한다.
        List<LeagueSeasonStandings> leagueSeasonStandings = findLeagueSeasonStandings(leagueSeasons);

        // 순위 정보를 조회한다.
        List<LeagueSeasonStandingsDataDocument> leagueSeasonStandingsDataDocuments = findStandingsDocuments(leagueSeasonStandings);

        Map<ExternalTeamStandingKey, List<TeamStandingSummary>> standingsByExternalTeamKey = new HashMap<>();

        for (LeagueSeasonStandingsDataDocument document : leagueSeasonStandingsDataDocuments) {
            // 순위 정보에서 visibleInMatchList=true 인 그룹만 별도 추출
            Map<String, GroupMapping> groupMappings = getVisibleGroupMappings(document);

            // visibleInMatchList=true 인 그룹에 대해서 순위 정보들을 추출
            standingsByExternalTeamKey.putAll(teamStandingExtractorRegistry.extract(document, groupMappings));
        }

        // 외부 팀 키를 내부 팀 ID로 한 번에 변환한다.
        // 반환값을 평면 Map으로 만들어 아래 매칭 로직이 중첩 Map 구조를 알 필요가 없도록 한다.
        Map<ExternalTeamStandingKey, Long> internalTeamIdsByExternalKey = findInternalTeamIdsByExternalKey(
                standingsByExternalTeamKey.keySet());

        // 추출한 순위 중 실제 조회된 경기에 참가하는 팀만 내부 팀 ID와 매칭한다.
        Map<LeagueTeamKey, List<TeamStandingSummary>> standingsByLeagueTeamKey = new HashMap<>();

        for (Map.Entry<ExternalTeamStandingKey, List<TeamStandingSummary>> entry : standingsByExternalTeamKey.entrySet()) {
            ExternalTeamStandingKey externalKey = entry.getKey();
            Long internalTeamId = internalTeamIdsByExternalKey.get(externalKey);

            if (internalTeamId == null
                    || !teamIdsByLeague.getOrDefault(externalKey.getLeagueId(), Set.of()).contains(internalTeamId)) {
                continue;
            }

            standingsByLeagueTeamKey.put(
                    new LeagueTeamKey(externalKey.getLeagueId(), internalTeamId),
                    sortByGroupOrder(entry.getValue()));
        }

        return standingsByLeagueTeamKey;
    }

    private List<LeagueSeasonStandings> findLeagueSeasonStandings(List<LeagueSeason> leagueSeasons) {
        List<Long> leagueSeasonIds = leagueSeasons.stream()
                .map(LeagueSeason::getId)
                .toList();

        if (leagueSeasonIds.isEmpty()) {
            return List.of();
        }

        return leagueSeasonStandingsRepository.findAllByLeagueSeasonIds(leagueSeasonIds)
                .stream()
                .filter(lss -> lss.getStandingsType() == LeagueSeasonStandingsType.EXTERNAL_DATA)
                .toList();
    }

    private List<LeagueSeason> findLeagueSeasons(Collection<Long> leagueIds, LocalDate date) {
        return leagueSeasonRepository.findAllByLeagueIdAndDate(leagueIds, date);
    }

    private List<LeagueSeasonStandingsDataDocument> findStandingsDocuments(List<LeagueSeasonStandings> leagueSeasonStandings) {
        List<Long> leagueSeasonStandingsIds = leagueSeasonStandings.stream()
                .map(LeagueSeasonStandings::getId)
                .toList();

        if (leagueSeasonStandingsIds.isEmpty()) {
            return List.of();
        }

        return leagueSeasonStandingsMongoRepository.findAllByLeagueSeasonStandingsIdIn(leagueSeasonStandingsIds);
    }

    private Map<String, GroupMapping> getVisibleGroupMappings(LeagueSeasonStandingsDataDocument document) {
        if (document.getGroupMappings() == null) {
            return Map.of();
        }

        return document.getGroupMappings().stream()
                .filter(GroupMapping::isVisibleInMatchList)
                .collect(Collectors.toUnmodifiableMap(GroupMapping::getGroupKey, Function.identity()));
    }

    private Map<ExternalTeamStandingKey, Long> findInternalTeamIdsByExternalKey(
            Collection<ExternalTeamStandingKey> externalKeys) {

        Map<DataOrigin, Set<String>> externalTeamIdsByProvider = groupExternalTeamIdsByProvider(externalKeys);
        Map<ExternalTeamStandingKey, Long> internalTeamIdsByExternalKey = new HashMap<>();

        // Repository 메서드는 provider 하나와 여러 externalTeamId를 받으므로
        // provider 수만큼만 일괄 조회한다. 팀 수만큼 조회하지 않는다.
        for (Map.Entry<DataOrigin, Set<String>> entry : externalTeamIdsByProvider.entrySet()) {
            DataOrigin provider = entry.getKey();
            Set<String> externalTeamIds = entry.getValue();

            List<TeamExternalMapping> mappings = teamExternalMappingRepository
                    .findAllByProviderAndApiTeamIdIn(provider, externalTeamIds);
            Map<String, Long> internalTeamIdsByExternalTeamId = toInternalTeamIdsByExternalTeamId(mappings);

            for (ExternalTeamStandingKey externalKey : externalKeys) {
                if (externalKey.getDataOrigin() != provider) {
                    continue;
                }

                Long internalTeamId = internalTeamIdsByExternalTeamId.get(externalKey.getExternalTeamId());
                if (internalTeamId != null) {
                    internalTeamIdsByExternalKey.put(externalKey, internalTeamId);
                }
            }
        }

        return internalTeamIdsByExternalKey;
    }

    private Map<DataOrigin, Set<String>> groupExternalTeamIdsByProvider(
            Collection<ExternalTeamStandingKey> externalKeys) {

        Map<DataOrigin, Set<String>> externalTeamIdsByProvider = new EnumMap<>(DataOrigin.class);

        for (ExternalTeamStandingKey externalKey : externalKeys) {
            externalTeamIdsByProvider
                    .computeIfAbsent(externalKey.getDataOrigin(), provider -> new HashSet<>())
                    .add(externalKey.getExternalTeamId());
        }

        return externalTeamIdsByProvider;
    }

    private Map<String, Long> toInternalTeamIdsByExternalTeamId(List<TeamExternalMapping> mappings) {
        Map<String, Long> internalTeamIdsByExternalTeamId = new HashMap<>();

        for (TeamExternalMapping mapping : mappings) {
            internalTeamIdsByExternalTeamId.put(mapping.getApiTeamId(), mapping.getInternalTeamId());
        }

        return internalTeamIdsByExternalTeamId;
    }

    private List<TeamStandingSummary> sortByGroupOrder(List<TeamStandingSummary> standings) {
        return standings.stream()
                .sorted(comparing(
                        TeamStandingSummary::getGroupOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                TeamStandingSummary::getGroupName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

}
