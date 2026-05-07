package com.scorenow.scorenow_api.domain.stadium.service;

import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumExternalMappingRepository stadiumExternalMappingRepository;
    private final SportExternalMappingRepository sportExternalMappingRepository;

    /**
     * 모든 경기장 조회
     */
    @Transactional(readOnly = true)
    public List<Stadium> getAllStadiums() {
        return stadiumRepository.findAll();
    }

    /**
     * 경기장명으로 경기장 조회
     */
    @Transactional(readOnly = true)
    public List<Stadium> getStadiumByStadiumName(final String stadiumName) {
        return stadiumRepository.findByNameContainingIgnoreCase(stadiumName);
    }

    /**
     * 경기장ID로 경기장 조회
     */
    @Transactional(readOnly = true)
    public Optional<Stadium> getStadiumByStadiumId(final Long stadiumId) {
        return stadiumRepository.findById(stadiumId);
    }

    /**
     * 경기장 생성
     */
    @Transactional
    public Stadium getOrCreateStadium(final ApiProvider provider, final String apiSportId, final String apiStadiumId, final String name, final String city) {
        // 1. 전달받은 외부 정보를 기반으로 Stadium External Mapping 테이블에 데이터가 있는지 확인
        Optional<StadiumExternalMapping> stadiumMappingInfo = stadiumExternalMappingRepository.findByExternalInfo(provider, apiSportId, apiStadiumId);

        // 2. 매핑 정보에서 internal stadium id 를 추출하고, 이를 기반으로 stadium 엔티티 반환
        if (stadiumMappingInfo.isPresent()) {
            Long internalStadiumId = stadiumMappingInfo.get().getInternalStadiumId();
            return stadiumRepository.findById(internalStadiumId).orElseGet(() -> createAndMapStadium(provider, apiSportId, apiStadiumId, name, city));
        }

        return createAndMapStadium(provider, apiSportId, apiStadiumId, name, city);
    }

    private Stadium createAndMapStadium(ApiProvider provider, String apiSportId, String apiStadiumId, String name, String city) {
        SportExternalMapping sportExternalMapping = sportExternalMappingRepository.findByProviderAndApiSportId(provider, apiSportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPORT_NOT_FOUND));

        log.info("[DB Miss ❌] 새로운 경기장입니다. DB 에 등록. → provider={}, apiSportId={}, apiStadiumId={}", provider, apiSportId, apiStadiumId);

        Stadium saved = stadiumRepository.save(Stadium.of(name, sportExternalMapping.getInternalSportId(), city));
        stadiumExternalMappingRepository.save(StadiumExternalMapping.of(provider, apiSportId, apiStadiumId, saved.getId()));

        return saved;
    }
}
