package com.scorenow.scorenow_api.domain.stadium.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scorenow.scorenow_api.domain.sport.repository.SportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumExternalMapping;
import com.scorenow.scorenow_api.domain.stadium.repository.FakeSportExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.FakeStadiumExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.FakeStadiumRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumExternalMappingRepository;
import com.scorenow.scorenow_api.domain.stadium.repository.StadiumRepository;
import com.scorenow.scorenow_api.external.common.ApiProvider;

class StadiumCacheServiceTest {

	private StadiumCacheService stadiumCacheService;
	private StadiumService stadiumService;

	private FakeStadiumRepository stadiumRepository;
	private FakeStadiumExternalMappingRepository stadiumExternalMappingRepository;
	private FakeSportExternalMappingRepository sportExternalMappingRepository;

	private Cache<StadiumCacheKey, Stadium> stadiumCache;

	@BeforeEach
	void setUp() {
		stadiumRepository = new FakeStadiumRepository();
		stadiumCache = Caffeine.newBuilder().build();
		stadiumService = new StadiumService(stadiumRepository, stadiumExternalMappingRepository,
			sportExternalMappingRepository);
		stadiumCacheService = new StadiumCacheService(stadiumCache, stadiumService);
	}

	@Test
	void 캐시와_DB에_존재하지_않는_경기장인_경우_DB에_저장_후_캐시에도_저장() {
		// 외부 API 에서 내려준 경기장 정보
		String externalSportId = "37";
		String externalStadiumId = "1234";
		String externalName = "서울상암월드컵경기장";
		String externalCity = "서울";

		// 캐시에 존재하지 않는다.
		Stadium beforeCached = stadiumCache.getIfPresent(
			new StadiumCacheKey(ApiProvider.BETS, externalSportId, externalStadiumId));
		assertThat(beforeCached).isNull();

		// Mapping Table에 존재하지 않는다.
		Optional<StadiumExternalMapping> stadiumMappingInfo = stadiumExternalMappingRepository.findByExternalInfo(
			ApiProvider.BETS, externalSportId, externalStadiumId);
		assertThat(stadiumMappingInfo.isEmpty()).isTrue();

		// DB 및 캐시에 저장
		Stadium savedStadium = stadiumCacheService.getOrCreateStadium(ApiProvider.BETS, externalSportId,
			externalStadiumId, externalName, externalCity);

		// 캐싱 되었는지 검증
		assertThat(stadiumCache.getIfPresent(new StadiumCacheKey(ApiProvider.BETS, externalSportId, externalStadiumId)))
			.isNotNull();

		// DB에 저장되었는지 검증 (StadiumExternalMapping, Stadium)
		assertThat(stadiumRepository.findById(savedStadium.getId()))
			.isPresent();
	}

	@Test
	void 캐시에는_없지만_DB에_존재하는_경기장인_경우_DB에서_조회_후_캐시에_저장() {
		// 외부 API 에서 내려준 경기장 정보
		String externalSportId = "37";
		String externalStadiumId = "1234";
		String externalName = "서울상암월드컵경기장";
		String externalCity = "서울";

		// 캐시에 존재하지 않는다.
		Stadium beforeCached = stadiumCache.getIfPresent(
			new StadiumCacheKey(ApiProvider.BETS, externalSportId, externalStadiumId));
		assertThat(beforeCached).isNull();

		// DB 에 미리 저장한다.
		Stadium savedStadium = stadiumService.getOrCreateStadium(ApiProvider.BETS, externalSportId, externalStadiumId,
			externalName, externalCity);

		// 검증 대상 메서드 호출
		stadiumCacheService.getOrCreateStadium(ApiProvider.BETS, externalSportId, externalStadiumId, externalName,
			externalCity);

		// DB 저장하는 메서드 미수행 검증 → 이미 저장된 데이터이기 때문에 추가 삽입이 발생하지 않음을 검증한다.
		assertThat(stadiumRepository.getSize()).isEqualTo(1);

		// 캐싱 되었는지 검증
		assertThat(stadiumCache.getIfPresent(new StadiumCacheKey(ApiProvider.BETS, externalSportId, externalStadiumId)))
			.isNotNull();
	}

	@Test
	void 캐시에_있는_경기장인_경우_DB에서_조회하거나_저장하는_로직이_실행되지_않는다() {
		// Mocking
		StadiumRepository mockStadiumRepository = mock(StadiumRepository.class);
		StadiumExternalMappingRepository mockStadiumExternalMappingRepository = mock(
			StadiumExternalMappingRepository.class);
		SportExternalMappingRepository mockSportExternalMappingRepository = mock(SportExternalMappingRepository.class);
		Cache mockStadiumCache = mock(Cache.class);

		// 의존성 주입
		stadiumService = new StadiumService(mockStadiumRepository, mockStadiumExternalMappingRepository,
			mockSportExternalMappingRepository);
		stadiumCacheService = new StadiumCacheService(mockStadiumCache, stadiumService);

		// 경기장 데이터 선언
		String externalSportId = "37";
		String externalStadiumId = "1234";
		String externalName = "서울상암월드컵경기장";
		String externalCity = "서울";

		Long internalSportId = 17L;

		Stadium stadium = Stadium.of(externalName, internalSportId, externalCity);
		StadiumExternalMapping stadiumExternalMapping = StadiumExternalMapping.of(ApiProvider.BETS, externalSportId,
			externalStadiumId, internalSportId);

		// 캐싱이 되어있는 경기장임을 가정
		when(mockStadiumCache.get(any(), any()))
			.thenReturn(stadium);

		// DB에 이미 저장되어있는 경기장임을 가정
		when(mockStadiumRepository.findById(any()))
			.thenReturn(Optional.of(stadium));

		when(mockStadiumExternalMappingRepository.findByExternalInfo(any(), any(), any()))
			.thenReturn(Optional.of(stadiumExternalMapping));

		// 검증 대상 메서드 호출
		stadiumCacheService.getOrCreateStadium(ApiProvider.BETS, externalSportId, externalStadiumId, externalName,
			externalCity);

		// 캐시 1회 조회 검증
		verify(mockStadiumCache, times(1)).get(any(), any());

		// 캐싱 미수행 검증 (이미 Hit 되었으므로)
		verify(mockStadiumCache, never()).put(any(), any());

		// DB 저장 미수행 검증 (이미 캐시 Hit 되었으므로)
		verify(mockStadiumRepository, never()).save(any());
		verify(mockStadiumExternalMappingRepository, never()).save(any());
	}

}