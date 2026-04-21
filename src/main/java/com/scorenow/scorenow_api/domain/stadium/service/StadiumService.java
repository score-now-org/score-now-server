package com.scorenow.scorenow_api.domain.stadium.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StadiumService {

    private final Cache<StadiumCacheKey, Stadium> stadiumCache;
    private final StadiumRepository stadiumRepository;

    /**
     * 모든 경기장 조회
     */
    public List<Stadium> getAllStadiums() {
        return stadiumRepository.findAll();
    }

    /**
     * 외부API의 경기장ID 를 통한 경기장 조회 <br>
     * 참고: {종목ID + 경기장ID} → 복합키 구조로 인해 경기장ID 로 조회하면 종목의 수에 따라서 다건의 결과를 반환할 수 있음.
     */
    public List<Stadium> getStadiumByExternalStadiumId(final String externalStadiumId) {
        return stadiumRepository.findByExternalStadiumId(externalStadiumId);
    }

    /**
     * 경기장명으로 경기장 조회
     */
    public List<Stadium> getStadiumByStadiumName(final String stadiumName) {
        return stadiumRepository.findByNameContainingIgnoreCase(stadiumName);
    }

    /**
     * 경기장 생성 (캐시 + DB 활용)
     */
    @Transactional
    public Stadium getOrCreateStadium(final Stadium stadium) {
        StadiumCacheKey cacheKey = StadiumCacheKey.generateKeyFrom(stadium);

        return stadiumCache.get(cacheKey, key -> stadiumRepository.findByExternalStadiumIdAndSportId(stadium.getExternalStadiumId(), stadium.getSportId())
                .map(existing -> {
                    log.info("[Cache Miss ❌/ DB Hit ✅] DB에 존재하는 경기장입니다. 캐시에 등록. → sportId={}, externalStadiumId={}", stadium.getSportId(), stadium.getExternalStadiumId());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("[Cache Miss ❌/ DB Miss ❌] 새로운 경기장입니다. DB 및 캐시에 등록. → sportId={}, externalStadiumId={}", stadium.getSportId(), stadium.getExternalStadiumId());
                    return stadiumRepository.save(stadium);
                }));
    }

}
