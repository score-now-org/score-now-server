package com.scorenow.scorenow_api.domain.stadium.service;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumUpdateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumResponse;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminStadiumService {

    private final StadiumRepository stadiumRepository;
    private final SportRepository sportRepository;

    @Transactional
    public AdminStadiumResponse createStadium(AdminStadiumCreateRequest request) {
        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        Stadium stadium = Stadium.of(
                requireText(request.getName(), "경기장명"),
                sport.getId(),
                requireText(request.getCity(), "도시명"),
                MANUAL
        );

        return AdminStadiumResponse.from(stadiumRepository.save(stadium), sport);
    }

    /**
     * 경기장 검색
     *  - 경기장 ID / 경기장명
     *  -
     */
    public Page<AdminStadiumResponse> searchStadiums(StadiumSearchCondition condition, Pageable pageable) {
        StadiumSearchCondition safeCondition = condition != null ? condition : new StadiumSearchCondition();

        Page<Stadium> stadiumPage = stadiumRepository.searchStadiums(
                safeCondition.getStadiumId(),
                normalize(safeCondition.getName()),
                safeCondition.getSportId(),
                pageable
        );

        // 조회한 경기장들에 대해 sportId 추출
        Set<Long> sportIds = stadiumPage.getContent().stream()
                .map(Stadium::getSportId)
                .collect(Collectors.toSet());

        // sportId 기반으로 각 Sport 엔티티 조회
        Map<Long, Sport> sportById = sportRepository.findAllById(sportIds).stream()
                .collect(Collectors.toMap(Sport::getId, Function.identity()));

        return stadiumPage.map(stadium -> {
            Sport sport = sportById.get(stadium.getSportId());
            if (sport == null) {
                throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
            }
            return AdminStadiumResponse.from(stadium, sport);
        });
    }

    /**
     * 경기장 관리 화면 선택 옵션 조회
     */
    public AdminStadiumSearchOptionsResponse getSearchOptions() {
        var sports = sportRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(sport -> AdminStadiumSearchOptionsResponse.SportOption.builder()
                        .id(sport.getId())
                        .code(sport.getSportCode().name())
                        .name(sport.resolveSportName())
                        .build())
                .toList();

        return AdminStadiumSearchOptionsResponse.builder()
                .sports(sports)
                .build();
    }

    /**
     * 경기장 정보 수정
     */
    @Transactional
    public void updateStadium(Long stadiumId, AdminStadiumUpdateRequest request) {
        Stadium stadium = stadiumRepository.findActiveById(stadiumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        applyUpdates(stadium, request);
    }

    /**
     * 수동 등록 경기장 삭제
     */
    @Transactional
    public void deleteStadium(Long stadiumId) {
        Stadium stadium = stadiumRepository.findActiveById(stadiumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        if (stadium.getDataOrigin() != MANUAL) {
            throw new BusinessException(ErrorCode.STADIUM_DELETE_NOT_ALLOWED);
        }

        stadium.deactivate();
    }

    private void applyUpdates(Stadium stadium, AdminStadiumUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수정할 경기장 정보가 없습니다.");
        }
        if (request.getName() == null && request.getCity() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수정할 값이 하나 이상 필요합니다.");
        }

        if (request.getName() != null) {
            stadium.updateName(requireText(request.getName(), "경기장명"));
        }
        if (request.getCity() != null) {
            stadium.updateCity(requireText(request.getCity(), "도시명"));
        }
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, fieldName + "은(는) 비워둘 수 없습니다.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
