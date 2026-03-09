package com.scorenow.scorenow_api.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {
    private final RestTemplate restTemplate;

    // 동일 IP 결과 캐싱 (최대 1000개, 24시간 TTL)
    private final Cache<String, String> ipCountryCache = Caffeine.newBuilder()
            .maximumSize(1000) //캐시에 저장 가능한 IP 최대 개수
            .expireAfterWrite(24, TimeUnit.HOURS)  //저장된 지 24시간 지난 항목 삭제
            .build();

    public String getCountryFromIp(String ip) {
        // 유효하지 않은 IP 조기 반환
        if (!isValidIp(ip)) {
            log.debug("Invalid or local IP skipped: {}", ip);
            return "";
        }
        // 이미 저장된 IP로 요청이 오면 외부API 호출X
        String cached = ipCountryCache.getIfPresent(ip);
        if (cached != null) {
            return cached;
        }
        return fetchCountryFromApi(ip);
    }

    /**
     * ip -> 국가코드 반환 API호출(외부)
     *
     * EX) 요청 : https://ipapi.co/1.2.3.4/country/
     *     응답 : "KR" 단순 국가코드 문자열 반환
     * @param ip
     * @return
     */
    private String fetchCountryFromApi(String ip) {
        try {
            String url = "https://ipapi.co/" + ip + "/country/";
            String country = restTemplate.getForObject(url, String.class);

            String result = (country != null && !country.isBlank()) ? country.trim() : "";

            ipCountryCache.put(ip, result); // 결과 캐싱
            return result;

        } catch (HttpClientErrorException e) {
            // 4xx: Rate Limit(429) 등 클라이언트 오류
            log.warn("GeoIP API client error for ip={}, status={}", ip, e.getStatusCode());
            return "";

        } catch (HttpServerErrorException e) {
            // 5xx: 외부 서버 오류
            log.error("GeoIP API server error for ip={}, status={}", ip, e.getStatusCode());
            return "";

        } catch (ResourceAccessException e) {
            // 네트워크 타임아웃, 연결 불가
            log.error("GeoIP API network error for ip={}", ip, e);
            return "";
        }
    }

    // localhost, 내부망 IP 필터링
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        return !ip.equals("127.0.0.1")
                && !ip.equals("::1")
                && !ip.startsWith("192.168.")
                && !ip.startsWith("10.")
                && !ip.startsWith("172.");
    }
}
