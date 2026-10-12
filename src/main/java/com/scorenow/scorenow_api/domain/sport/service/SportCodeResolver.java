package com.scorenow.scorenow_api.domain.sport.service;

import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SportCodeResolver {

    private final SportRepository sportRepository;

    public SportCode resolveByInternalSportId(Long internalSportId) {
        Sport sport = sportRepository.findById(internalSportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        return sport.getSportCode();
    }

}

