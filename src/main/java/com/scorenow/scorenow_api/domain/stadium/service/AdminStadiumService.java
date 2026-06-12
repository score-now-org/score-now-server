package com.scorenow.scorenow_api.domain.stadium.service;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumUpdateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumResponse;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.MANUAL;

@Slf4j
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

        Stadium stadium = Stadium.of(request.getName(), sport.getId(), request.getCity(), MANUAL);

        return AdminStadiumResponse.from(stadiumRepository.save(stadium));
    }

    /**
     * 경기장 검색
     */
    public List<AdminStadiumResponse> searchStadiums(StadiumSearchCondition condition) {
        StadiumSearchCondition safeCondition = condition != null ? condition : new StadiumSearchCondition();

        return stadiumRepository.searchStadiums(
                        safeCondition.getStadiumId(),
                        normalize(safeCondition.getName())
                )
                .stream()
                .map(AdminStadiumResponse::from)
                .toList();
    }

    /**
     * 경기장 정보 수정
     */
    @Transactional
    public void updateStadium(Long stadiumId, AdminStadiumUpdateRequest request) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STADIUM_NOT_FOUND));

        applyUpdates(stadium, request);
    }

    private void applyUpdates(Stadium stadium, AdminStadiumUpdateRequest request) {
        if (request == null) {
            log.info("❌경기장 업데이트 정보가 없습니다. stadiumId={}, stadiumName={}", stadium.getId(), stadium.getName());
            return;
        }

        if (request.getName() != null) {
            stadium.updateName(request.getName());
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
