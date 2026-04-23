package com.scorenow.scorenow_api.domain.stadium.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.domain.stadium.repository.FakeStadiumRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StadiumServiceTest {

    private static final String FOOTBALL = "1";

    private StadiumService stadiumService;
    private FakeStadiumRepository stadiumRepository;
    private Cache<StadiumCacheKey, Stadium> stadiumCache;

    @BeforeEach
    void setUp() {
        stadiumRepository = new FakeStadiumRepository();
        stadiumCache = Caffeine.newBuilder().build();
        stadiumService = new StadiumService(stadiumCache, stadiumRepository);
    }

    @Test
    void 캐시와_DB에_존재하지_않는_경기장인_경우_DB에_저장_후_캐시에도_저장() {
        String externalStadiumId = "BETS-STADIUM-001";
        String sportId = FOOTBALL;

        Stadium stadium = Stadium.of(externalStadiumId, "서울상암월드컵경기장", sportId, "서울");

        // 캐시에 존재하지 않는다.
        Stadium beforeCached = stadiumCache.getIfPresent(StadiumCacheKey.generateKeyFrom(stadium));
        assertThat(beforeCached).isNull();

        // DB에 존재하지 않는다.
        Optional<Stadium> beforeSaved = stadiumRepository.findByExternalStadiumIdAndSportId(externalStadiumId, sportId);
        assertThat(beforeSaved.isEmpty()).isTrue();

        // DB 및 캐시에 저장
        Stadium saved = stadiumService.getOrCreateStadium(stadium);

        // 캐싱 되었는지 검증
        assertThat(stadiumCache.getIfPresent(StadiumCacheKey.generateKeyFrom(stadium)))
                .isNotNull();

        // DB에 저장되었는지 검증
        assertThat(stadiumRepository.findByExternalStadiumIdAndSportId(externalStadiumId, sportId))
                .isPresent();
    }

    @Test
    void 캐시에는_없지만_DB에_존재하는_경기장인_경우_DB에서_조회_후_캐시에_저장() {
        String externalStadiumId = "BETS-STADIUM-001";
        String sportId = FOOTBALL;

        Stadium stadium = Stadium.of(externalStadiumId, "서울상암월드컵경기장", sportId, "서울");

        // 캐시에 존재하지 않는다.
        Stadium beforeCached = stadiumCache.getIfPresent(StadiumCacheKey.generateKeyFrom(stadium));
        assertThat(beforeCached).isNull();

        Stadium saved = stadiumRepository.save(stadium);

        stadiumService.getOrCreateStadium(stadium);

        // DB 저장하는 메서드 미수행 검증 → 이미 저장된 데이터이기 때문에 추가 삽입이 발생하지 않음을 검증한다.
        assertThat(stadiumRepository.getSize()).isEqualTo(1);

        // 캐싱 되었는지 검증
        assertThat(stadiumCache.getIfPresent(StadiumCacheKey.generateKeyFrom(stadium)))
                .isNotNull();
    }

    @Test
    void 캐시에_있는_경기장인_경우_DB에서_조회하거나_저장하는_로직이_실행되지_않는다() {
        StadiumRepository mockStadiumRepository = mock(StadiumRepository.class);
        Cache mockStadiumCache = mock(Cache.class);
        stadiumService = new StadiumService(mockStadiumCache, mockStadiumRepository);

        Stadium stadium = Stadium.of("BETS-STADIUM-001", "서울상암월드컵경기장", FOOTBALL, "서울");

        // 캐싱이 되어있는 경기장임을 가정
        when(mockStadiumCache.get(any(), any()))
                .thenReturn(stadium);

        // DB에 이미 저장되어있는 경기장임을 가정
        when(mockStadiumRepository.findByExternalStadiumIdAndSportId(any(), any()))
                .thenReturn(Optional.of(stadium));

        stadiumService.getOrCreateStadium(stadium);

        // 캐시 1회 조회 검증
        verify(mockStadiumCache, times(1)).get(any(), any());

        // 캐싱 미수행 검증 (이미 Hit 되었으므로)
        verify(mockStadiumCache, never()).put(any(), any());

        // DB 저장 미수행 검증 (이미 캐시 Hit 되었으므로)
        verify(mockStadiumRepository, never()).save(any());
    }

}