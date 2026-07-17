package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLeagueSeasonService {

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueRepository leagueRepository;

    /**
     * 리그 시즌 생성
     */
    @Transactional
    public void createLeagueSeason(LeagueSeasonCreateRequest request) {

        Long leagueId = request.getLeagueId();
        String seasonName = normalize(request.getSeasonName());

        // 리그 존재 여부 검증
        if (!leagueRepository.existsById(leagueId)) {
            throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
        }

        // 리그 ID + 시즌명으로 기등록 데이터 검증
        if (leagueSeasonRepository.existsByLeagueIdAndSeasonName(leagueId, seasonName)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미 해당 리그에 등록된 시즌명입니다.");
        }

        // 시즌 일정 유효성 검증 (startAt <= endAt)
        LocalDate seasonStartAt = request.getSeasonStartAt();
        LocalDate seasonEndAt = request.getSeasonEndAt();
        if (seasonStartAt.isAfter(seasonEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌 종료일이 시즌 시작일보다 과거일 수 없습니다.");
        }

        // 리그와 연계된 시즌들중 현재 등록하고자 하는 시즌 정보와 일정이 겹치는 시즌이 있는지 확인
        List<LeagueSeason> leagueSeasons = leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(leagueId);
        for (LeagueSeason leagueSeason : leagueSeasons) {
            if (leagueSeason.overlapsWith(seasonStartAt, seasonEndAt)) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌 일정이 겹치는 시즌이 존재합니다.");
            }
        }

        // 새로운 "현재 시즌" 등록의 경우 기존 "현재 시즌" 을 해제
        if (request.isCurrent()) {
            leagueSeasonRepository.findCurrentSeasonByLeagueId(leagueId)
                    .ifPresent(leagueSeason -> leagueSeason.updateCurrent(false));
        }

        // 등록
        LeagueSeason leagueSeason = LeagueSeason.create(
                leagueId,
                seasonName,
                request.getSeasonStartAt(), request.getSeasonEndAt(),
                request.isCurrent());

        leagueSeasonRepository.save(leagueSeason);
    }

    /**
     * 리그 시즌 검색
     * 조회 조건 : 전체 조회, 리그 ID, 리그 명
     * 정렬 순서 : 현재 시즌 -> 시즌 시작일 최근순)
     */
    public Page<AdminLeagueSeasonResponse> searchLeagueSeasons(
            LeagueSeasonSearchCondition condition,
            Pageable pageable) {

        LeagueSeasonSearchCondition safeCondition = condition != null
                ? condition
                : new LeagueSeasonSearchCondition();

        return leagueSeasonRepository.searchLeagueSeasons(
                safeCondition.getLeagueId(),
                normalize(safeCondition.getLeagueName()),
                pageable);
    }

    /**
     * 리그 시즌 수정
     */
    @Transactional
    public void updateLeagueSeason(Long leagueSeasonId, LeagueSeasonUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수정 요청 정보는 필수입니다.");
        }

        // 시즌 존재 여부 확인
        LeagueSeason target = leagueSeasonRepository.findByIdAndIsActiveTrue(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시즌 정보를 찾을 수 없습니다."));

        // 리그의 모든 시즌을 조회 (시즌명 중복 및 일정 중복 검증 용도)
        List<LeagueSeason> seasons = leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(target.getLeagueId());

        // 시즌명 변경 시, 시즌명 중복 여부 확인 (같은 리그 한해서만)
        String newSeasonName = normalize(request.getSeasonName()) != null
                ? normalize(request.getSeasonName())
                : target.getSeasonName();

        validateDuplicateSeasonName(seasons, target, newSeasonName);
        target.updateSeasonName(newSeasonName);

        // 시즌 시작일, 종료일 변경 시, 시즌 일정 유효성 검증 (startAt<=endAt / 일정 겹치는지 / 단, 대상 시즌(target) 제외)
        LocalDate newSeasonStartAt = request.getSeasonStartAt() != null
                ? request.getSeasonStartAt()
                : target.getSeasonStartAt();

        LocalDate newSeasonEndAt = request.getSeasonEndAt() != null
                ? request.getSeasonEndAt()
                : target.getSeasonEndAt();

        validateOverlappingPeriod(seasons, target, newSeasonStartAt, newSeasonEndAt);
        target.updatePeriod(newSeasonStartAt, newSeasonEndAt);

        // 현재 시즌 설정 시, 기존 다른 현재 시즌 설정 해제
        if (request.getIsCurrent() != null) {
            if (Boolean.TRUE.equals(request.getIsCurrent())) {
                seasons.stream()
                        .filter(season -> !season.isSameId(target.getId())) // Target 은 제외
                        .filter(LeagueSeason::isCurrent)    // '현재 시즌' 인 시즌을 필터링
                        .forEach(season -> season.updateCurrent(false));    // '현재 시즌' false 로 변경
            }

            target.updateCurrent(request.getIsCurrent());
        }
    }

    /**
     * 리그 시즌 삭제
     */
    @Transactional
    public void deleteLeagueSeason(Long leagueSeasonId) {
        LeagueSeason leagueSeason = leagueSeasonRepository.findByIdAndIsActiveTrue(leagueSeasonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시즌 정보를 찾을 수 없습니다."));

        leagueSeason.deactivate();
    }

    private void validateOverlappingPeriod(
            List<LeagueSeason> seasons,
            LeagueSeason target,
            LocalDate newSeasonStartAt,
            LocalDate newSeasonEndAt) {

        boolean overlapped = seasons.stream()
                .filter(season -> !season.isSameId(target.getId()))
                .anyMatch(season -> season.overlapsWith(newSeasonStartAt, newSeasonEndAt));

        if (overlapped) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "시즌 일정이 겹치는 시즌이 존재합니다.");
        }
    }

    private void validateDuplicateSeasonName(
            List<LeagueSeason> seasons,
            LeagueSeason target,
            String newSeasonName) {

        boolean isSeasonNameDuplicated = seasons.stream()
                .filter(season -> !season.isSameId(target.getId()))
                .anyMatch(season -> season.hasSameSeasonName(newSeasonName));

        if (isSeasonNameDuplicated) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미 해당 리그에 등록된 시즌명입니다.");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
