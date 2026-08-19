package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchCandidateResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchesResponse;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatch;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.FeaturedMatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFeaturedMatchService {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("uuuuMMdd");

    private final MatchRepository matchRepository;
    private final FeaturedMatchRepository featuredMatchRepository;

    /**
     * 상단고정/핫매치 등록
     */
    @Transactional
    public FeaturedMatchesResponse createFeaturedMatch(List<Long> matchIds, FeaturedMatchType type) {

        // matchIds 유효성 검증 (빈값 여부, 중복값 여부)
        validateMatchIds(matchIds);

        // 경기 존재 여부 확인
        List<Match> matches = matchRepository.findAllByIdsOrderByStartAt(matchIds);

        if (matches.size() != matchIds.size()) {
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }

        // 상단고정/핫매치 기등록 여부 확인
        if (!featuredMatchRepository.findRegisteredMatchIds(matchIds).isEmpty()) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_ALREADY_EXISTS);
        }

        // 경기 시작 일자 세팅 및 전체 동일 여부 확인
        LocalDate displayDate = getDisplayDate(matches);

        // 상단고정/핫매치 설정 등록
        List<FeaturedMatch> saveTargets = new ArrayList<>();
        int displayOrder = getNextDisplayOrder(displayDate, type);

        for (Match match : matches) {
            FeaturedMatch featuredMatch = new FeaturedMatch(match.getId(), displayDate, type, displayOrder++);
            saveTargets.add(featuredMatch);
        }

        featuredMatchRepository.saveAll(saveTargets);

        return getFeaturedMatches(displayDate);
    }

    /**
     * 상단고정/핫매치 등록 대상 경기 조회
     */
    public List<FeaturedMatchCandidateResponse> searchFeaturedMatchCandidates(LocalDate matchStartDate) {

        LocalDateTime start = matchStartDate.atStartOfDay();
        LocalDateTime endExclusive = matchStartDate.plusDays(1).atStartOfDay();

        // 특정 일자에 예정된 모든 활성화된 경기를 조회
        List<Match> candidateMatches = matchRepository.findMatchesByDateRange(start, endExclusive);

        List<Long> candidateMatchIds = candidateMatches.stream()
                .map(Match::getId)
                .toList();

        // 조회된 경기 중 이미 상단고정/핫매치로 등록된 matchId 조회
        Set<Long> registeredMatchIds = candidateMatchIds.isEmpty()
                ? Set.of()
                : new HashSet<>(featuredMatchRepository.findRegisteredMatchIds(candidateMatchIds));

        return candidateMatches.stream()
                .map(match -> new FeaturedMatchCandidateResponse(
                        match.getId(),
                        match.getSportId(),
                        match.getSport().resolveSportName(),
                        match.getLeague().resolveLeagueName(),
                        match.getStartAt(),
                        match.getHomeTeam().resolveTeamName(),
                        match.getAwayTeam().resolveTeamName(),
                        !registeredMatchIds.contains(match.getId())))
                .toList();

    }

    /**
     * 상단고정/핫매치 등록 경기 조회
     */
    public FeaturedMatchesResponse searchFeaturedMatches(LocalDate displayDate) {
        return getFeaturedMatches(displayDate);
    }

    /**
     * 상단고정/핫매치 순서 변경
     */
    @Transactional
    public FeaturedMatchesResponse updateFeaturedMatchesOrder(LocalDate displayDate,
                                                              FeaturedMatchType type,
                                                              List<Long> featuredMatchIds) {

        updateOrder(displayDate, type, featuredMatchIds);

        return getFeaturedMatches(displayDate);
    }

    /**
     * 전달받은 orderedIds 를 기반으로 상단고정/핫매치 순서 재정렬
     */
    private void updateOrder(LocalDate displayDate, FeaturedMatchType type, List<Long> reorderedIds) {
        if (reorderedIds == null) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER);
        }

        List<FeaturedMatch> featuredMatches = featuredMatchRepository.findByDisplayDateAndTypeOrderByDisplayOrderAsc(displayDate, type);

        // 순서 재정렬 이전과 이후의 개수 동일 여부 검증 (순서만 바뀔 뿐, 총 개수는 동일)
        if (featuredMatches.size() != reorderedIds.size()) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER, "순서 변경 대상 개수가 일치하지 않습니다.");
        }

        // 중복되는 ID 가 있는지 확인
        Set<Long> reorderedFeaturedMatchIds = new HashSet<>(reorderedIds);

        if (reorderedFeaturedMatchIds.size() != reorderedIds.size()) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER, "중복인 상단고정/핫매치 정보가 존재합니다.");
        }

        // 같은 ID 로 구성되어 있는지 검증
        Set<Long> currentFeaturedMatchIds = featuredMatches.stream()
                .map(FeaturedMatch::getId)
                .collect(Collectors.toSet());


        if (!currentFeaturedMatchIds.equals(reorderedFeaturedMatchIds)) {
            throw new BusinessException(ErrorCode.FEATURED_MATCH_INVALID_ORDER, "순서 변경 대상이 올바르지 않습니다.");
        }

        // 순서 업데이트를 위한 Map<FeaturedMatchId, displayOrder> 를 생성
        Map<Long, Integer> displayOrderByFeaturedMatchId = new HashMap<>();
        for (int i = 0; i < reorderedIds.size(); i++) {
            displayOrderByFeaturedMatchId.put(reorderedIds.get(i), i + 1);
        }

        // 순서 업데이트
        for (FeaturedMatch featuredMatch : featuredMatches) {
            Integer newDisplayOrder = displayOrderByFeaturedMatchId.get(featuredMatch.getId());
            featuredMatch.updateDisplayOrder(newDisplayOrder);
        }

    }

    /**
     * 상단고정/핫매치 해제 (삭제)
     */
    @Transactional
    public FeaturedMatchesResponse deleteFeaturedMatch(Long featuredMatchId) {

        // 삭제 대상 유효성 여부 판단
        FeaturedMatch featuredMatch = featuredMatchRepository.findById(featuredMatchId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FEATURED_MATCH_NOT_FOUND));

        // 삭제
        featuredMatchRepository.delete(featuredMatch);

        Integer deletedDisplayOrder = featuredMatch.getDisplayOrder();
        LocalDate displayDate = featuredMatch.getDisplayDate();
        FeaturedMatchType type = featuredMatch.getType();

        // 삭제 후 재정렬
        reorderDisplayOrder(displayDate, type, deletedDisplayOrder);

        return getFeaturedMatches(displayDate);
    }

    /**
     * 경기 일정 변경 시, 상단고정/핫매치 해제
     */
    @Transactional
    public void deleteFeaturedMatchByMatchId(Long matchId) {

        featuredMatchRepository.findByMatchId(matchId)
                .ifPresent(featuredMatch -> {
                    featuredMatchRepository.delete(featuredMatch);

                    Integer deletedDisplayOrder = featuredMatch.getDisplayOrder();
                    LocalDate displayDate = featuredMatch.getDisplayDate();
                    FeaturedMatchType type = featuredMatch.getType();

                    reorderDisplayOrder(displayDate, type, deletedDisplayOrder);
                });
    }

    /**
     * 일자 기준으로 상단고정/핫매치 경기 조회
     */
    private FeaturedMatchesResponse getFeaturedMatches(LocalDate displayDate) {

        List<FeaturedMatchResponse> hotMatches = new ArrayList<>();
        List<FeaturedMatchResponse> pinnedMatches = new ArrayList<>();

        featuredMatchRepository.findByDisplayDateWithMatch(displayDate)
                .stream()
                .map(FeaturedMatchResponse::from)
                .forEach(response -> {
                    if (FeaturedMatchType.HOT_MATCH.name().equals(response.getType())) {
                        hotMatches.add(response);
                    } else if (FeaturedMatchType.PINNED.name().equals(response.getType())) {
                        pinnedMatches.add(response);
                    }
                });

        return new FeaturedMatchesResponse(displayDate.format(YYYYMMDD), hotMatches, pinnedMatches);
    }

    /**
     * 특정 일자의 상단고정/핫매치 마지막 순번 반환
     */
    private int getNextDisplayOrder(LocalDate displayDate, FeaturedMatchType type) {
        return featuredMatchRepository.findMaxDisplayOrder(displayDate, type).orElse(0) + 1;
    }

    /**
     * 삭제가 발생했을 때, 순서를 재정렬
     */
    private void reorderDisplayOrder(LocalDate displayDate, FeaturedMatchType type, Integer deletedDisplayOrder) {

        // 삭제한 FeaturedMatch 의 displayOrder 보다 후순위에 위치한 FeaturedMatch 조회
        List<FeaturedMatch> featuredMatches = featuredMatchRepository
                .findMatchesAfterDisplayOrder(displayDate, type, deletedDisplayOrder);

        featuredMatches.forEach(FeaturedMatch::moveUpDisplayOrder);
    }

    /**
     * 조회 기준 일자 반환
     * - 경기 시작 일자 세팅 여부 검증
     * - 조회 기준 일자 동일 여부 검증
     */
    private LocalDate getDisplayDate(List<Match> matches) {

        LocalDate displayDate = null;

        for (Match match : matches) {
            if (match.getStartAt() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "경기 시작 일자가 존재하지 않습니다.");
            }

            if (displayDate == null) {
                displayDate = match.getStartAt().toLocalDate();
            }

            if (!displayDate.equals(match.getStartAt().toLocalDate())) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        "경기 시작 일자가 서로 다릅니다. 기준날짜:" + displayDate + ", 조회경기날짜:" + match.getStartAt().toLocalDate());
            }
        }

        return displayDate;
    }

    private void validateMatchIds(List<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "등록할 경기 ID는 필수입니다.");
        }

        if (new HashSet<>(matchIds).size() != matchIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "중복된 경기 ID가 존재합니다.");
        }
    }
}
