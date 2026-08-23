package com.scorenow.scorenow_api.domain.stadium.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scorenow.scorenow_api.domain.sport.entity.SportExternalMapping;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.stadium.repository.*;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class StadiumCacheServiceTest {

    private StadiumCacheService stadiumCacheService;
    private StadiumService stadiumService;

    private FakeStadiumRepository stadiumRepository;
    private FakeStadiumExternalMappingRepository stadiumExternalMappingRepository;
    private FakeSportExternalMappingRepository sportExternalMappingRepository;

    private Cache<StadiumCacheKey, Long> stadiumCache;

    // 테스트용 공통 상수 추출
    private static final DataOrigin PROVIDER = DataOrigin.BETS;
    private static final String EXT_SPORT_ID = "37";
    private static final String EXT_STADIUM_ID = "1234";
    private static final String EXT_NAME = "서울상암월드컵경기장";
    private static final String EXT_CITY = "서울";

    @BeforeEach
    void setUp() {
        // 의존성 주입 (Fake 객체 생성)
        stadiumRepository = new FakeStadiumRepository();
        stadiumExternalMappingRepository = new FakeStadiumExternalMappingRepository();
        sportExternalMappingRepository = new FakeSportExternalMappingRepository();

        stadiumCache = Caffeine.newBuilder().build();
        stadiumService = new StadiumService(stadiumRepository, stadiumExternalMappingRepository, sportExternalMappingRepository);
        stadiumCacheService = new StadiumCacheService(stadiumCache, stadiumService);

        // sport 매핑 데이터 생성
        sportExternalMappingRepository.save(SportExternalMapping.of(PROVIDER, EXT_SPORT_ID, 1L));
    }

    @Test
    void 캐시와_DB에_존재하지_않는_경기장은_DB에_저장하고_내부_경기장_ID를_캐싱한다() {

        // 캐시에 존재하지 않는다.
        Long beforeCached = stadiumCache.getIfPresent(new StadiumCacheKey(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID));
        assertThat(beforeCached).isNull();

        // Mapping Table에 존재하지 않는다.
        Optional<StadiumExternalMapping> stadiumMappingInfo = stadiumExternalMappingRepository.findByExternalInfo(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID);
        assertThat(stadiumMappingInfo.isEmpty()).isTrue();

        // DB 및 캐시에 저장
        Long savedStadiumId = stadiumCacheService.getOrCreateStadiumId(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID, EXT_NAME, EXT_CITY);

        // 캐싱 되었는지 검증
        assertThat(stadiumCache.getIfPresent(new StadiumCacheKey(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID)))
                .isEqualTo(savedStadiumId);

        // DB에 저장되었는지 검증 (StadiumExternalMapping, Stadium)
        assertThat(stadiumRepository.findById(savedStadiumId))
                .isPresent();

    }

    @Test
    void 캐시에는_없지만_DB에_존재하는_경기장인_경우_DB에서_조회_후_캐시에_저장() {

        // 캐시에 존재하지 않는다.
        Long beforeCached = stadiumCache.getIfPresent(new StadiumCacheKey(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID));
        assertThat(beforeCached).isNull();

        // DB 에 미리 저장한다. (StadiumExternalMapping, Stadium)
        Stadium savedStadium = stadiumRepository.save(Stadium.of(EXT_NAME, 1L, EXT_CITY));
        stadiumExternalMappingRepository.save(StadiumExternalMapping.of(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID, savedStadium.getId()));

        // 검증 대상 메서드 호출
        stadiumCacheService.getOrCreateStadiumId(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID, EXT_NAME, EXT_CITY);

        // DB 저장하는 메서드 미수행 검증 → 이미 저장된 데이터이기 때문에 추가 삽입이 발생하지 않음을 검증한다.
        assertThat(stadiumRepository.getSize()).isEqualTo(1);

        // 캐싱 되었는지 검증
        assertThat(stadiumCache.getIfPresent(new StadiumCacheKey(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID)))
                .isNotNull();

    }

    @Test
    void 캐시에_있는_경기장인_경우_DB에서_조회하거나_저장하는_로직이_실행되지_않는다() {
        // Mocking
        StadiumRepository mockStadiumRepository = mock(StadiumRepository.class);
        StadiumExternalMappingRepository mockStadiumExternalMappingRepository = mock(StadiumExternalMappingRepository.class);
        SportExternalMappingRepository mockSportExternalMappingRepository = mock(SportExternalMappingRepository.class);
        Cache<StadiumCacheKey, Long> mockStadiumCache = mock(Cache.class);

        // 의존성 주입
        stadiumService = new StadiumService(mockStadiumRepository, mockStadiumExternalMappingRepository, mockSportExternalMappingRepository);
        stadiumCacheService = new StadiumCacheService(mockStadiumCache, stadiumService);

        Long internalStadiumId = 17L;

        Stadium stadium = Stadium.of(EXT_NAME, 1L, EXT_CITY);
        StadiumExternalMapping stadiumExternalMapping = StadiumExternalMapping.of(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID, internalStadiumId);

        // 캐싱이 되어있는 경기장임을 가정
        when(mockStadiumCache.get(any(), any()))
                .thenReturn(internalStadiumId);

        // DB에 이미 저장되어있는 경기장임을 가정
        when(mockStadiumRepository.findById(any()))
                .thenReturn(Optional.of(stadium));

        when(mockStadiumExternalMappingRepository.findByExternalInfo(any(), any(), any()))
                .thenReturn(Optional.of(stadiumExternalMapping));

        // 검증 대상 메서드 호출
        stadiumCacheService.getOrCreateStadiumId(DataOrigin.BETS, EXT_SPORT_ID, EXT_STADIUM_ID, EXT_NAME, EXT_CITY);

        // 캐시 1회 조회 검증
        verify(mockStadiumCache, times(1)).get(any(), any());

        // 캐싱 미수행 검증 (이미 Hit 되었으므로)
        verify(mockStadiumCache, never()).put(any(), any());

        // DB 저장 미수행 검증 (이미 캐시 Hit 되었으므로)
        verify(mockStadiumRepository, never()).save(any());
        verify(mockStadiumExternalMappingRepository, never()).save(any());
    }


}
