package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.league.service.standings.TeamStandingLookupService;
import com.scorenow.scorenow_api.domain.league.service.standings.model.LeagueTeamKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.*;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatch;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.FeaturedMatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.appresponse.MatchAppSportResponseMapperRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchAppQueryService {

    private final MatchStatusDisplayResolver matchStatusDisplayResolver;
    private final MatchAppSportResponseMapperRegistry matchAppSportResponseMapperRegistry;

    private final TeamStandingLookupService teamStandingLookupService;

    private final MatchRepository matchRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final FeaturedMatchRepository featuredMatchRepository;
    private final StadiumRepository stadiumRepository;

    /**
     * 앱 경기 상세 조회
     */
    public MatchAppDetailResponse getMatch(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_DETAIL_NOT_FOUND));

        Stadium stadium = match.getStadiumId() != null
                ? stadiumRepository.findById(match.getStadiumId()).orElse(null)
                : null;

        Long leagueId = match.getLeagueId();

        Map<Long, Set<Long>> teamIdsByLeagueId = new HashMap<>();
        teamIdsByLeagueId.put(leagueId, Set.of(match.getHomeId(), match.getAwayId()));

        Map<LeagueTeamKey, List<TeamStandingSummary>> teamStandings = teamStandingLookupService.getTeamStandings(teamIdsByLeagueId, match.getStartAt().toLocalDate());
        List<TeamStandingSummary> homeTeamStandings = teamStandings.getOrDefault(new LeagueTeamKey(leagueId, match.getHomeId()), List.of());
        List<TeamStandingSummary> awayTeamStandings = teamStandings.getOrDefault(new LeagueTeamKey(leagueId, match.getAwayId()), List.of());

        return MatchAppDetailResponse.from(
                match,
                stadium,
                matchDetailDocument,
                matchStatusDisplayResolver.resolve(match, matchDetailDocument),
                homeTeamStandings,
                awayTeamStandings,
                matchAppSportResponseMapperRegistry.mapDetail(matchDetailDocument)
        );
    }

    /**
     * 앱 경기 목록 조회 (상단고정/핫매치)
     */
    public MatchAppResponse getMatchesWithFeatured(LocalDate date, SportCode sportCode, Long leagueId) {

        LocalDate targetDate = resolveDate(date);

        List<MatchAppLeagueGroupResponse> leagueGroupResponses = getMatches(targetDate, sportCode, leagueId);

        // Map<matchId, MatchAppLeagueGroupResponse> 생성 : matchId 로 해당 경기가 속한 리그 정보를 쉽게 찾기 위해
        Map<Long, MatchAppLeagueGroupResponse> leagueGroupByMatchId = new HashMap<>();
        for (MatchAppLeagueGroupResponse leagueGroupResponse : leagueGroupResponses) {
            List<MatchAppItemResponse> matches = leagueGroupResponse.getMatches();

            matches.forEach(match -> {
                Long matchId = match.getId();
                leagueGroupByMatchId.put(matchId, leagueGroupResponse);
            });
        }

        // Map<matchId, MatchAppItemResponse> 생성 : matchId 로 기존 응답 DTO 를 쉽게 찾기 위해
        Map<Long, MatchAppItemResponse> matchItemByMatchId = leagueGroupResponses.stream()
                .flatMap(leagueGroup -> leagueGroup.getMatches().stream())
                .collect(Collectors.toUnmodifiableMap(
                        MatchAppItemResponse::getId,
                        Function.identity()
                ));

        // 상단고정/핫매치 추출 (기존 조회한 Match 기반으로)
        Set<Long> matchIds = matchItemByMatchId.keySet();
        List<FeaturedMatch> featuredMatches = matchIds.isEmpty()
                ? List.of()
                : featuredMatchRepository.findAllByDisplayDateAndMatchIds(targetDate, matchIds);

        List<MatchAppFeaturedItemResponse> pinnedMatches = new ArrayList<>();
        List<MatchAppFeaturedItemResponse> hotMatches = new ArrayList<>();

        for (FeaturedMatch featuredMatch : featuredMatches) {
            Long matchId = featuredMatch.getMatchId();

            if (!matchItemByMatchId.containsKey(matchId)) {
                continue;
            }

            MatchAppItemResponse matchItem = matchItemByMatchId.get(matchId);
            MatchAppLeagueGroupResponse leagueGroup = leagueGroupByMatchId.get(matchId);

            MatchAppFeaturedItemResponse featuredItem = MatchAppFeaturedItemResponse.builder()
                    .leagueId(leagueGroup.getLeagueId())
                    .leagueName(leagueGroup.getLeagueName())
                    .match(matchItem)
                    .build();

            if (featuredMatch.getType() == FeaturedMatchType.PINNED) {
                pinnedMatches.add(featuredItem);
            } else if (featuredMatch.getType() == FeaturedMatchType.HOT_MATCH) {
                hotMatches.add(featuredItem);
            }
        }

        // 응답 조립
        MatchAppFeaturedSectionResponse featuredSectionResponse = MatchAppFeaturedSectionResponse.builder()
                .pinnedMatches(pinnedMatches)
                .hotMatches(hotMatches)
                .build();

        return MatchAppResponse.builder()
                .date(targetDate)
                .featuredMatches(featuredSectionResponse)
                .leagueMatches(leagueGroupResponses)
                .build();
    }

    /**
     * 앱 경기 목록 조회
     */
    private List<MatchAppLeagueGroupResponse> getMatches(LocalDate date, SportCode sportCode, Long leagueId) {
        LocalDate targetDate = resolveDate(date);
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        // 경기 목록을 조회
        List<Match> matches = matchRepository.findAppMatches(
                startAt,
                endAt,
                sportCode,
                leagueId,
                MatchAppStatusGroup.allStatuses(),
                MatchAppStatusGroup.IN_PLAY.getStatuses(),
                MatchAppStatusGroup.SCHEDULED.getStatuses(),
                MatchAppStatusGroup.ENDED.getStatuses()
        );

        if (matches.isEmpty()) {
            log.info("🟠해당 날짜 {} 에 대하여 조회되는 경기가 없습니다. sportCode={}, leagueId={}", date, sportCode, leagueId);
            return List.of();
        }

        Map<Long, Stadium> stadiumById = loadStadiumsById(matches);

        // 경기 목록 조회 결과 기반으로 경기 상세 정보를 조회
        List<Long> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        Map<Long, MatchDetailDocument> matchDetailById = matchDetailRepository.findAllById(matchIds).stream()
                .collect(toMap(
                        MatchDetailDocument::getId,
                        Function.identity(),
                        (first, second) -> first));

        // 조회 정렬 순서를 유지하면서 리그별 경기 목록으로 그룹핑
        Map<Long, List<Match>> matchesByLeague = new LinkedHashMap<>();
        Map<Long, Set<Long>> teamIdsByLeague = new LinkedHashMap<>();

        for (Match match : matches) {
            matchesByLeague.computeIfAbsent(match.getLeagueId(), newLeagueId -> new ArrayList<>())
                    .add(match);

            Set<Long> teamIds = teamIdsByLeague.computeIfAbsent(match.getLeagueId(), newLeagueId -> new HashSet<>());
            teamIds.add(match.getHomeId());
            teamIds.add(match.getAwayId());
        }

        Map<LeagueTeamKey, List<TeamStandingSummary>> teamStandings = teamStandingLookupService.getTeamStandings(
                teamIdsByLeague,
                targetDate);

        // 리그 정보를 최상위로 두고, 각 리그 하위에 경기 목록을 반환
        return matchesByLeague.values().stream()
                .map(leagueMatches -> {
                    List<MatchAppItemResponse> appMatches = leagueMatches.stream()
                            .map(match -> {
                                MatchDetailDocument matchDetail = matchDetailById.get(match.getId());

                                return MatchAppItemResponse.from(
                                        match,
                                        match.getSport().getSportCode(),
                                        findStadium(stadiumById, match.getStadiumId()),
                                        matchDetail,
                                        matchStatusDisplayResolver.resolve(match, matchDetail),
                                        teamStandings.getOrDefault(
                                                new LeagueTeamKey(match.getLeagueId(), match.getHomeId()),
                                                List.of()),
                                        teamStandings.getOrDefault(
                                                new LeagueTeamKey(match.getLeagueId(), match.getAwayId()),
                                                List.of()),
                                        matchAppSportResponseMapperRegistry.mapLiveSummary(match, matchDetail)
                                );
                            })
                            .toList();

                    Match firstMatch = leagueMatches.get(0);
                    return MatchAppLeagueGroupResponse.from(
                            firstMatch.getLeague(),
                            firstMatch.getSport().getSportCode(),
                            appMatches
                    );
                })
                .toList();
    }

    private LocalDate resolveDate(LocalDate date) {
        return date != null ? date : LocalDate.now(ZoneId.of(SEOUL_TIME_ZONE));
    }

    private Stadium findStadium(Map<Long, Stadium> stadiumById, Long stadiumId) {
        return stadiumId == null ? null : stadiumById.get(stadiumId);
    }

    private Map<Long, Stadium> loadStadiumsById(List<Match> matches) {
        Set<Long> stadiumIds = matches.stream()
                .filter(match -> match.getTemporaryStadium() == null)
                .map(Match::getStadiumId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (stadiumIds.isEmpty()) {
            return Map.of();
        }

        return stadiumRepository.findAllByIds(stadiumIds).stream()
                .collect(toMap(
                        Stadium::getId,
                        Function.identity(),
                        (first, second) -> first));
    }

}
